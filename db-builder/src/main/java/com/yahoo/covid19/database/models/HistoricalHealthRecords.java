/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database.models;

import com.yahoo.covid19.database.DatabaseBuilder;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Data
@Slf4j
public class HistoricalHealthRecords extends LatestHealthRecords {
    public static final String TABLE_NAME = "health_records";
    private String numPositiveTests;
    private String numDeaths;
    private String numRecoveredCases;
    private String diffNumPositiveTests;
    private String diffNumDeaths;
    private String avgWeeklyDeaths;
    private String avgWeeklyConfirmedCases;
    private String avgWeeklyRecoveredCases;

    @Override
    protected String getInsertStatement() {
        return String.format(
                "INSERT INTO %s ("
                        + "id, label, referenceDate, regionId, longitude, latitude, wikiId, dataSource, "
                        + "totalDeaths, totalConfirmedCases, totalRecoveredCases, totalTestedCases, "
                        + "numPositiveTests, numDeaths, numRecoveredCases, diffNumPositiveTests, diffNumDeaths, "
                        + "avgWeeklyDeaths, avgWeeklyConfirmedCases, avgWeeklyRecoveredCases) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                getTableName());
    }

    @Override
    protected PreparedStatement getStatement(DatabaseBuilder.DBConnector connector) throws SQLException {
        PreparedStatement preparedStatement = super.getStatement(connector);
        preparedStatement.setObject(13, numPositiveTests == null ? null : Long.valueOf(numPositiveTests));
        preparedStatement.setObject(14, numDeaths == null ? null : Long.valueOf(numDeaths));
        preparedStatement.setObject(15, numRecoveredCases == null ? null : Long.valueOf(numRecoveredCases));
        preparedStatement.setObject(16, diffNumPositiveTests == null ? null : Long.valueOf(diffNumPositiveTests));
        preparedStatement.setObject(17, diffNumDeaths == null ? null : Long.valueOf(diffNumDeaths));
        preparedStatement.setObject(18, avgWeeklyDeaths == null ? null : Double.valueOf(avgWeeklyDeaths));
        preparedStatement.setObject(19, avgWeeklyConfirmedCases == null ? null : Double.valueOf(avgWeeklyConfirmedCases));
        preparedStatement.setObject(20, avgWeeklyRecoveredCases == null ? null : Double.valueOf(avgWeeklyRecoveredCases));
        return preparedStatement;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }
}
