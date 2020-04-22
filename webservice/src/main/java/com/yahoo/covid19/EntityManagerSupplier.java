/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19;

import com.yahoo.covid19.database.DBUtils;
import com.yahoo.covid19.models.Country;
import com.yahoo.covid19.models.County;
import com.yahoo.covid19.models.HealthRecords;
import com.yahoo.covid19.models.LatestHealthRecords;
import com.yahoo.covid19.models.Metadata;
import com.yahoo.covid19.models.State;
import com.yahoo.elide.standalone.PersistenceUnitInfoImpl;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import static com.yahoo.covid19.database.DatabaseBuilder.PATH_SEPARATOR;

/**
 * Entity manager supplier that rebuilds the Entity manager factory on a periodic basis.
 */
@Slf4j
public class EntityManagerSupplier implements Supplier<EntityManager>  {

    private EntityManagerFactory emf;

    public EntityManagerSupplier(File baseDbDirectory) {
        emf = buildEntityManagerFactory(baseDbDirectory);
    }

    public static EntityManagerFactory buildEntityManagerFactory(File dbLocation) {
        List<String> classNames = new ArrayList<>();
        classNames.add(Country.class.getName());
        classNames.add(County.class.getName());
        classNames.add(State.class.getName());
        classNames.add(HealthRecords.class.getName());
        classNames.add(LatestHealthRecords.class.getName());
        classNames.add(Metadata.class.getName());


        Properties options = new Properties();
        options.put("hibernate.show_sql", "false");
        options.put("hibernate.hbm2ddl.auto", "validate");
        options.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        options.put("hibernate.current_session_context_class", "thread");
        options.put("hibernate.jdbc.use_scrollable_resultset", "true");

        // Collection Proxy & JDBC Batching
        options.put("hibernate.jdbc.batch_size", "50");
        options.put("hibernate.jdbc.fetch_size", "50");
        options.put("hibernate.default_batch_fetch_size", "100");

        // Hikari Connection Pool Settings
        options.put("hibernate.connection.provider_class", "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
        options.put("hibernate.hikari.connectionTimeout", "20000");
        options.put("hibernate.hikari.maximumPoolSize", "100");
        options.put("hibernate.hikari.idleTimeout", "30000");
        options.put("hibernate.hikari.leakDetectionThreshold", "31000");
        options.put("hibernate.hikari.maxLifetime", "35000");

        options.put("javax.persistence.jdbc.driver", "org.h2.Driver");

        options.put("javax.persistence.jdbc.url",
                "jdbc:h2:"
                        + dbLocation
                        + PATH_SEPARATOR
                        + DBUtils.DB_NAME
                        + ";ACCESS_MODE_DATA=r");

        options.put("javax.persistence.jdbc.user", "");
        options.put("javax.persistence.jdbc.password", "");

        PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl("elide-covid19",
                        classNames, options);

        return new EntityManagerFactoryBuilderImpl(
                    new PersistenceUnitInfoDescriptor(persistenceUnitInfo), new HashMap<>()).build();
    }

    @Override
    public EntityManager get() {
        return emf.createEntityManager();
    }
}
