/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database;

public enum ErrorCodes {
    OK,
    INVALID_ID,
    INVALID_TYPE,
    INVALID_COORDINATE,
    INVALID_DATE,
    MISSING_FOREIGN_KEY,
    DANGLING_FOREIGN_KEY,
    MISMATCH_FOREIGN_KEY
}
