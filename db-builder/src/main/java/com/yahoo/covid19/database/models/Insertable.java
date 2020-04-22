/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database.models;

import com.yahoo.covid19.database.DatabaseBuilder;
import com.yahoo.covid19.database.ErrorCodes;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Represents a data record that is inserted into the H2 database.
 */
public interface Insertable {

    /**
     * Generates a Prepared Statement to insert this record.
     * @param connector The database connection.
     * @return A prepared statement with parameters filled in.
     * @throws SQLException
     */
    PreparedStatement getStatement(DatabaseBuilder.DBConnector connector) throws SQLException;

    /**
     * Validates the input record prior to DB insertion.
     * @return true if the record is valid.
     */
    boolean isValid(Map<String, Insertable> foreignKeyMap);

    ErrorCodes getErrorCode();

    Object getId();

    String getTableName();

    /**
     * Populate fields from other tables using foriegn key.
     * @param foreignKeyMap
     */
    void getForiegnKeyFields(Map<String, Insertable> foreignKeyMap);
}
