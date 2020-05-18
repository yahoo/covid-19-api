/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19;

import com.google.common.collect.Sets;

import com.yahoo.covid19.models.HealthRecords;
import com.yahoo.covid19.models.Place;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.Injector;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.CaseAwareJPQLGenerator;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.security.executors.BypassPermissionExecutor;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.FileCopyUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;

import static com.yahoo.covid19.database.DBUtils.DB_DIR_NAME;
import static com.yahoo.covid19.database.DBUtils.DB_FILE_NAME;
import static com.yahoo.covid19.database.DatabaseBuilder.PATH_SEPARATOR;

/**
 * Elide API to serve Covid-19 API.
 */
@SpringBootApplication
public class App {
    private ScheduledExecutorService executor;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(App.class, args);
    }

    @PostConstruct
    public void init(){
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Bean(name = "databaseDirectory")
    public File getDatabaseDirectory() throws IOException {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Bean
    public Elide initializeElide(EntityDictionary dictionary,
                                 DataStore dataStore, ElideConfigProperties settings) {

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withDefaultMaxPageSize(settings.getMaxPageSize())
                .withDefaultPageSize(settings.getPageSize())
                .withUseFilterExpressions(true)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withAuditLogger(new Slf4jLogger())
                .withPermissionExecutor(BypassPermissionExecutor.class) //Permissions enforced in controller.
                .withEncodeErrorResponses(true)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));


        FilterTranslator.registerJPQLGenerator(Operator.IN_INSENSITIVE, HealthRecords.class, "placeId",
                new CaseAwareJPQLGenerator(
                        "%s IN (%s)",
                        CaseAwareJPQLGenerator.Case.NONE,
                        CaseAwareJPQLGenerator.ArgumentCount.MANY));
        FilterTranslator.registerJPQLGenerator(Operator.IN_INSENSITIVE, Place.class, "id",
                new CaseAwareJPQLGenerator(
                        "%s IN (%s)",
                        CaseAwareJPQLGenerator.Case.NONE,
                        CaseAwareJPQLGenerator.ArgumentCount.MANY));

        return new Elide(builder.build());
    }

    /**
     * Override to disable Spring injection on beans (which is expensive for high RPS).
     * @param beanFactory
     * @return
     */
    @Bean
    public EntityDictionary buildDictionary(AutowireCapableBeanFactory beanFactory) {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>(),
                new Injector() {
                    @Override
                    public void inject(Object entity) {
                    }

                    @Override
                    public <T> T instantiate(Class<T> cls) {
                        try {
                            return cls.newInstance();
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                });

        dictionary.scanForSecurityChecks();
        return dictionary;
    }

    @Bean(name = "databaseFile")
    public File getDatabaseFile(@Qualifier("databaseDirectory") File databaseDirectory) throws IOException {
        InputStream stream = getClass().getResourceAsStream(
                PATH_SEPARATOR + DB_DIR_NAME + PATH_SEPARATOR + DB_FILE_NAME);

        File destinationFile = new File(databaseDirectory, DB_FILE_NAME);
        destinationFile.delete();

        FileCopyUtils.copy(stream, new FileOutputStream(destinationFile));

        Files.setPosixFilePermissions(destinationFile.toPath(), Sets.newHashSet(PosixFilePermission.OWNER_READ));
        return new File(databaseDirectory, DB_FILE_NAME);
    }

    @Bean
    public DataStore buildDataStore(@Qualifier("databaseFile") File dbFile) throws IOException {
        EntityManagerSupplier supplier = new EntityManagerSupplier(dbFile.getParentFile());

        return new JpaDataStore(
                () -> {
                    return supplier.get();
                },
                (em -> {
                    /* Make session read only for better performance. */
                    Session session = em.unwrap(Session.class);
                    session.setDefaultReadOnly(true);
                    session.setHibernateFlushMode(FlushMode.MANUAL);
                    return new NonJtaTransaction(em);
                }));

    }
}
