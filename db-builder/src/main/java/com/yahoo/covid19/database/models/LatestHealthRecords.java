/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database.models;

public class LatestHealthRecords extends HealthRecords {
    public static final String TABLE_NAME = "latest_health_records";

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }
}
