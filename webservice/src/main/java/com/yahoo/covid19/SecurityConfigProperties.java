/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "security")
public class SecurityConfigProperties {
    private String origin = "*";
    private boolean indexPageEnabled = true;
    private boolean cspEnabled = false;
    private CacheProperties cache;
    private WhiteListProperties whiteList;
}
