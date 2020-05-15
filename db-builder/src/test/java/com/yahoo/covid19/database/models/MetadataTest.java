/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */


package com.yahoo.covid19.database.models;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MetadataTest {

    @Test
    public void testDateExtraction() {
        HistoricalHealthRecords record1 = new HistoricalHealthRecords();
        HistoricalHealthRecords record2 = new HistoricalHealthRecords();
        HistoricalHealthRecords record3 = new HistoricalHealthRecords();
        record1.setReferenceDate("2020-07-01 00:00:02");
        record2.setReferenceDate("2020-07-01 00:00:00");
        record3.setReferenceDate("2020-07-03 00:00:00");
        List<Insertable> insertables = Arrays.asList(record1, record2, record3);

        Metadata metadata = new Metadata(insertables, new Date());
        assertEquals(metadata.getHealthRecordsEndDate(), "2020-07-03 00:00:00");
        assertEquals(metadata.getHealthRecordsStartDate(), "2020-07-01 00:00:00");
    }

    @Test
    public void testIsValid() {

        List<Insertable> insertables = Arrays.asList();

        Metadata metadata = new Metadata(insertables, new Date());
        assertFalse(metadata.isValid(new HashMap<>()));
    }
}
