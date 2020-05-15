/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database.models;

import com.yahoo.covid19.database.DatabaseBuilder;
import com.yahoo.covid19.database.ErrorCodes;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Used to return metadata about the DB.
 */
@Slf4j
@Data
public class Metadata implements Insertable {
    private static final String METADATA_INSERT_STATEMENT = "INSERT INTO metadata ("
                + "id, healthRecordsStartDate, healthRecordsEndDate, publishedDate) "
                + "VALUES (?, ?, ?, ?);";

    private final String id = "info";
    private final String healthRecordsStartDate;
    private final String healthRecordsEndDate;
    private final Timestamp publishedDate;

    public Metadata(List<Insertable> insertables, Date lastModifiedDate) {
        String minDate = null;
        String maxDate = null;

        for (Insertable insertable : insertables) {
            if (insertable.getTableName().equals(LatestHealthRecords.TABLE_NAME)
                    || insertable.getTableName().equals(HistoricalHealthRecords.TABLE_NAME)) {
                LatestHealthRecords record = (LatestHealthRecords) insertable;
                String currentDate = ((LatestHealthRecords) insertable).getReferenceDate();
                if (minDate == null) {
                    minDate = currentDate;
                    maxDate = currentDate;
                } else {
                    if (maxDate.compareTo(currentDate) < 0) {
                        maxDate = currentDate;
                    }

                    if (minDate.compareTo(currentDate) > 0) {
                        minDate = currentDate;
                    }
                }
            }
        }

        healthRecordsStartDate = minDate;
        healthRecordsEndDate = maxDate;
        publishedDate = new Timestamp(lastModifiedDate.getTime());
    }

    @Override
    public List<PreparedStatement> getStatements(DatabaseBuilder.DBConnector connector) throws SQLException {
        java.util.Date startDate = null;
        java.util.Date endDate = null;
        try {
            startDate = DatabaseBuilder.REFERENCE_DATE_FORMAT.parse(healthRecordsStartDate);
            endDate = DatabaseBuilder.REFERENCE_DATE_FORMAT.parse(healthRecordsEndDate);
        } catch (ParseException e) {
            // Should be handled in isValid
        }
        PreparedStatement statement = connector.getPreparedStatement(METADATA_INSERT_STATEMENT);
        statement.setString(1, id);
        statement.setString(2, DatabaseBuilder.DB_DATE_FORMAT.format(startDate));
        statement.setString(3, DatabaseBuilder.DB_DATE_FORMAT.format(endDate));
        statement.setTimestamp(4, publishedDate);
        return Arrays.asList(statement);
    }

    @Override
    public boolean isValid(Map<String, Insertable> foreignKeyMap) {
        return (healthRecordsEndDate != null && healthRecordsStartDate != null);
    }

    @Override
    public ErrorCodes getErrorCode() {
        return ErrorCodes.OK;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTableName() {
        return "metadata";
    }

    @Override
    public void getForiegnKeyFields(Map<String, Insertable> foreignKeyMap) {
    }
}
