/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database;

import com.google.common.io.Files;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseBuilderTest {
    private DatabaseBuilder builder;

    private void countCheck(DatabaseBuilder.DBConnector connector, String table, String where, Integer expected) throws SQLException {
        String whereClause = where == null || where.isEmpty() ? "" : " WHERE " + where;
        int rowCount = (int) connector.executeSQLQuery("SELECT COUNT(*) FROM " + table + whereClause + ";",
                resultSet -> {
                    try {
                        return resultSet.last() ? resultSet.getInt(1) : -1;
                    } catch (Exception e) {
                        return -1;
                    }
                }
        );
        assertEquals(expected, rowCount);
    }

    private void addInputStrem(String filename, DatabaseBuilder builder) throws URISyntaxException, FileNotFoundException {
        File file = new File(this.getClass().getResource(filename).toURI());
        builder.processInputStream(filename, new FileInputStream(file));
    }

    @BeforeAll
    public void setup() throws FileNotFoundException, URISyntaxException {
        File outputDirectory = Files.createTempDir();
        outputDirectory.deleteOnExit();
        builder = new DatabaseBuilder(outputDirectory, 10.0);
        addInputStrem("/data/metadata/region-metadata.json", builder);
        addInputStrem("/data/by-region-2020-04-03.json", builder);
        builder.build();
    }


    @Test
    public void test() throws Exception {
        try (DatabaseBuilder.DBConnector connector = builder.newDBConnector()) {
            countCheck(connector, "health_records", null, 2743);
            countCheck(connector, "health_records", "label = 'Earth'", 1);
            countCheck(connector, "country", null,204);
            countCheck(connector, "state", null,232);
            countCheck(connector, "county", null,3137);
        }
    }


    @Test
    public void testInvalidEntries() throws Exception {
        try (DatabaseBuilder.DBConnector connector = builder.newDBConnector()) {
            countCheck(connector, "health_records", "label != 'Earth' AND countryId IS NULL", 0);
            countCheck(connector, "state", "countryId IS NULL",0);
            countCheck(connector, "county", "stateId IS NULL OR countryId IS NULL",0);
        }
    }

    @Test
    public void testIndexCreation() throws Exception {
        PreparedStatement statement = null;
        try (DatabaseBuilder.DBConnector connector = builder.newDBConnector()) {
            int rowCount = (int) connector.executeSQLQuery("SELECT * FROM INFORMATION_SCHEMA.INDEXES;",
                    resultSet -> {
                        try {
                            return resultSet.last() ? resultSet.getRow() : 0;
                        } catch (Exception e) {
                            return 0;
                        }
                    }
            );
            // 22 configured indexes and 5 primary keys
            assertEquals(27, rowCount);
        }
    }
}
