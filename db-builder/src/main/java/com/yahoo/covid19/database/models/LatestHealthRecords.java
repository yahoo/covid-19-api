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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.yahoo.covid19.database.ErrorCodes.DANGLING_FOREIGN_KEY;
import static com.yahoo.covid19.database.ErrorCodes.INVALID_DATE;
import static com.yahoo.covid19.database.ErrorCodes.MISSING_FOREIGN_KEY;
import static com.yahoo.covid19.database.ErrorCodes.OK;


@Data
@Slf4j
public class LatestHealthRecords implements Insertable {
    public static final String TABLE_NAME = "latest_health_records";

    private UUID id = null;
    private String regionId;
    private String label;
    private String referenceDate;
    private String totalDeaths;
    private String totalConfirmedCases;
    private String totalRecoveredCases;
    private String totalTestedCases;
    private String dataSource;

    // Fields from foreign table
    private String wikiId;
    private List<String> regionTypes;
    private Double longitude;
    private Double latitude;

    private transient ErrorCodes errorCode = OK;

    private static String getNonNull(String name) {
        return name == null ? "" : name;
    }

    protected String getInsertStatement() {
        return String.format(
                "INSERT INTO %s ("
                + "id, label, referenceDate, regionId, longitude, latitude, wikiId, dataSource, "
                + "totalDeaths, totalConfirmedCases, totalRecoveredCases, totalTestedCases) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                getTableName());
    }

    protected PreparedStatement getStatement(DatabaseBuilder.DBConnector connector) throws SQLException {
        Date parsedReferenceDate = null;
        try {
            parsedReferenceDate = DatabaseBuilder.REFERENCE_DATE_FORMAT.parse(referenceDate);
        } catch (ParseException e) {
            // handled in is valid
        }
        PreparedStatement statement = connector.getPreparedStatement(getInsertStatement());
        statement.setObject(1, getId());
        statement.setString(2, label);
        statement.setString(3, DatabaseBuilder.DB_DATE_FORMAT.format(parsedReferenceDate));
        statement.setString(4, regionId);
        statement.setDouble(5, longitude);
        statement.setDouble(6, latitude);
        statement.setString(7, wikiId);
        statement.setString(8, dataSource);
        statement.setObject(9, totalDeaths == null ? null : Long.valueOf(totalDeaths));
        statement.setObject(10, totalConfirmedCases == null ? null : Long.valueOf(totalConfirmedCases));
        statement.setObject(11, totalRecoveredCases == null ? null : Long.valueOf(totalRecoveredCases));
        statement.setObject(12, totalTestedCases == null ? null : Long.valueOf(totalTestedCases));

        return statement;
    }

    @Override
    public List<PreparedStatement> getStatements(DatabaseBuilder.DBConnector connector) throws SQLException {
        return Arrays.asList(getStatement(connector));
    }

    private Places lookUpPlace(Map<String, Insertable> foreignKeyMap) {
        return (Places) foreignKeyMap.get(regionId);
    }

    @Override
    public boolean isValid(Map<String, Insertable> foreignKeyMap) {
        try {
            DatabaseBuilder.REFERENCE_DATE_FORMAT.parse(referenceDate);
        } catch (ParseException e) {
            errorCode = INVALID_DATE;
            return false;
        }

        if (regionId == null) {
            errorCode = MISSING_FOREIGN_KEY;
            return false;
        }
        Places place = lookUpPlace(foreignKeyMap);
        if (place == null || !place.isValid(foreignKeyMap)) {
            errorCode = DANGLING_FOREIGN_KEY;
            return false;
        }

        return true;
    }

    @Override
    public Object getId() {
        if (id == null) {
            id = UUID.nameUUIDFromBytes((
                    getNonNull(regionId) + referenceDate).getBytes());
        }
        return id;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public void getForiegnKeyFields(Map<String, Insertable> foreignKeyMap) {
        Places place = lookUpPlace(foreignKeyMap);

        this.latitude = place.getLatitude() == null ? 0.0 : Double.valueOf(place.getLatitude());
        this.longitude = place.getLongitude() == null ? 0.0 : Double.valueOf(place.getLongitude());
        this.wikiId = place.getWikiId();
        this.regionTypes = place.getType();
    }
}
