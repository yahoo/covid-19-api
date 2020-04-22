/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19;

import com.google.common.io.Files;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.File;

@TestConfiguration
public class AppTestSetup {

    @Bean
    @Primary
    public File getTestDatabaseDirectory() {
        return Files.createTempDir();
    }

}
