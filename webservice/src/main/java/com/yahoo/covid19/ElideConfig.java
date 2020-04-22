/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.Injector;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.contrib.swagger.SwaggerBuilder;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.spring.config.ElideConfigProperties;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.models.Info;
import io.swagger.models.Swagger;

import java.util.HashMap;
import java.util.TimeZone;

/**
 * Auto Configuration For Elide Services.  Override any of the beans (by defining your own) to change
 * the default behavior.
 */
@Configuration
public class ElideConfig {

    /**
     * Creates a singular swagger document for JSON-API.
     * @param elide Fully initialized elide instance.
     * @param settings Elide configuration settings.
     * @return An instance of a Swagger object
     */
    @Bean
    public Swagger buildSwagger(Elide elide, ElideConfigProperties settings) {
        Info info = new Info()
                .title(settings.getSwagger().getName())
                .version(settings.getSwagger().getVersion());

        SwaggerBuilder builder = new SwaggerBuilder(elide.getElideSettings().getDictionary(), info);

        Swagger swagger = builder.build();
        swagger.getPaths().forEach((url, path) -> {
            path.setDelete(null);
            path.setPost(null);
            path.setPatch(null);
        });

        return swagger.basePath(settings.getJsonApi().getPath());
    }
}
