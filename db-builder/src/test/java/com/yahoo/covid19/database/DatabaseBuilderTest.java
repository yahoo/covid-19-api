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

    private void countCheck(DatabaseBuilder.DBConnector connector, String table, Integer expected) throws SQLException {
        countCheck(connector, table, null, null, expected);
    }
    private void countCheck(DatabaseBuilder.DBConnector connector, String table, String where, Integer expected) throws SQLException {
        countCheck(connector, table, null, where, expected);
    }
    private void countCheck(DatabaseBuilder.DBConnector connector, String table, String join, String where, Integer expected) throws SQLException {
        String whereClause = where == null || where.isEmpty() ? "" : " WHERE " + where;
        String joinClause = join == null ? "" : " " + join;
        int rowCount = (int) connector.executeSQLQuery("SELECT COUNT(*) FROM " + table + joinClause + whereClause + ";",
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
        addInputStrem("/data/by-region-2020-04-24.json", builder);
        builder.build();
    }


    @Test
    public void test() throws Exception {
        try (DatabaseBuilder.DBConnector connector = builder.newDBConnector()) {
            countCheck(connector, "health_records", null, 3489);
            countCheck(connector, "health_records", "label = 'Earth'", 1);
            countCheck(connector, "health_records",
                    "LEFT JOIN relationship_hierarchy ON relationship_hierarchy.childId = health_records.regionId",
                    "health_records.label = 'Earth' AND relationship_hierarchy.parentId IS NULL", 1);

            countCheck(connector, "place", null,3801);
            countCheck(connector, "place", "label = 'Earth'", 1);
            countCheck(connector, "place",
                    "LEFT JOIN relationship_hierarchy ON relationship_hierarchy.childId = place.id",
                    "place.label = 'Earth' AND relationship_hierarchy.parentId IS NULL", 1);
        }
    }


    @Test
    public void testInvalidEntries() throws Exception {
        try (DatabaseBuilder.DBConnector connector = builder.newDBConnector()) {
            countCheck(connector, "health_records",
                    "LEFT JOIN relationship_hierarchy ON relationship_hierarchy.childId = health_records.regionId",
                    "health_records.label != 'Earth' AND relationship_hierarchy.parentId IS NULL", 0);
            countCheck(connector, "place",
                    "LEFT JOIN relationship_hierarchy ON relationship_hierarchy.childId = place.id",
                    "place.label != 'Earth' AND relationship_hierarchy.parentId IS NULL", 0);
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
            // 5 configured indexes
            // 1 primary key index for healthRecords
            // 1 primary key index for latestHealthRecords
            // 1 primary key index for places
            // 2 primary key index for relationship_hierarchy
            assertEquals(10, rowCount);
        }
    }
}
