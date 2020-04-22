/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19;

import lombok.Data;

@Data
public class CacheProperties {
    private boolean enabled = false;
    /**
     * Interval at which cache should be cleared.
     */
    private long duration = 60;
}
