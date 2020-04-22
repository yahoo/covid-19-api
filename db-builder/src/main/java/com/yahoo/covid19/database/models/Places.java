/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database.models;

import com.yahoo.covid19.database.DatabaseBuilder;
import com.yahoo.covid19.database.ErrorCodes;
import com.yahoo.covid19.database.JoinTableNames;
import com.yahoo.covid19.database.gsonAdapters.TableNameAdapter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Map;

import static com.yahoo.covid19.database.ErrorCodes.DANGLING_FOREIGN_KEY;
import static com.yahoo.covid19.database.ErrorCodes.INVALID_COORDINATE;
import static com.yahoo.covid19.database.ErrorCodes.INVALID_ID;
import static com.yahoo.covid19.database.ErrorCodes.INVALID_TYPE;
import static com.yahoo.covid19.database.ErrorCodes.MISSING_FOREIGN_KEY;
import static com.yahoo.covid19.database.ErrorCodes.OK;

import com.google.gson.annotations.JsonAdapter;

/**
 * Used to slurp County records from Yahoo Knowledge Graph.
 */
@Slf4j
@Data
public class Places implements Insertable {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String COUNTY_INSERT_STATEMENT = "INSERT INTO county ("
                + "id, label, wikiId, longitude, "
                + "latitude, population, stateId, countryId) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

    private static final String STATE_INSERT_STATEMENT = "INSERT INTO state ("
            + "id, label, wikiId, longitude, "
            + "latitude, population, countryId) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?);";

    private static final String COUNTRY_INSERT_STATEMENT = "INSERT INTO country ("
            + "id, label, wikiId, longitude, "
            + "latitude, population) "
            + "VALUES (?, ?, ?, ?, ?, ?);";

    private final String id;
    private final String label;
    private final String wikiId;
    private final String longitude;
    private final String latitude;
    private final String population;
    private final String stateId;
    private final String countryId;

    @JsonAdapter(TableNameAdapter.class)
    private final JoinTableNames type;

    private transient ErrorCodes errorCode = OK;


    private PreparedStatement getCommonStatement(DatabaseBuilder.DBConnector connector, String insert_template) throws SQLException {
        PreparedStatement statement = connector.getPreparedStatement(insert_template);
        statement.setString(1, id);
        statement.setString(2, label);
        statement.setString(3, wikiId);
        statement.setDouble(4, (longitude == null) ? 0 : Double.valueOf(longitude));
        statement.setDouble(5, (latitude == null) ? 0 : Double.valueOf(latitude));
        statement.setObject(6, population == null ? null : Long.valueOf(population));
        return statement;
    }

    @Override
    public PreparedStatement getStatement(DatabaseBuilder.DBConnector connector) throws SQLException {
        PreparedStatement statement;
        switch (type) {
            case Country:
            case Supername:
                statement = getCommonStatement(connector, COUNTRY_INSERT_STATEMENT);
                break;
            case StateAdminArea:
                statement = getCommonStatement(connector, STATE_INSERT_STATEMENT);
                statement.setString(7, countryId);
                break;
            case CountyAdminArea:
                statement = getCommonStatement(connector, COUNTY_INSERT_STATEMENT);
                statement.setString(7, stateId);
                statement.setString(8, countryId);
                break;
            default:
                throw new IllegalStateException("invalid geographical type: " + type);
        }

        return statement;
    }

    @Override
    public boolean isValid(Map<String, Insertable> foreignKeyMap) {
        if (id == null || id.isEmpty() || type == null) {
            if (type == null) {
                errorCode = INVALID_TYPE;
            } else {
                errorCode = INVALID_ID;
            }
            return false;
        }

        switch (type) {
            case StateAdminArea:
                return validateState(foreignKeyMap);
            case CountyAdminArea:
                return validateCounty(foreignKeyMap);
            case Country:
                return validateCountry(foreignKeyMap);
            case Supername:
                return validateEarth(foreignKeyMap);
            default:
                return false;
        }
    }

    public boolean isValidCoordinate(String coordinate) {
        if (coordinate == null) {
            errorCode = INVALID_COORDINATE;
            return false;
        }

        try {
            Double.valueOf(coordinate);
        } catch (NumberFormatException e) {
            errorCode = INVALID_COORDINATE;
            return false;
        }
        return true;
    }

    public boolean validateState(Map<String, Insertable> foreignKeyMap) {
        if (! isValidCoordinate(latitude) || ! isValidCoordinate(longitude)) {
            errorCode = INVALID_COORDINATE;
            return false;
        }

        if (countryId == null || countryId.isEmpty()) {
            errorCode = MISSING_FOREIGN_KEY;
            return false;
        }
        Places country = (Places) foreignKeyMap.get(countryId);
        if (country == null || !country.validateCountry(foreignKeyMap)) {
            errorCode = DANGLING_FOREIGN_KEY;
            return false;
        }

        return true;
    }

    public boolean validateCounty(Map<String, Insertable> foreignKeyMap) {
        if (! isValidCoordinate(latitude) || ! isValidCoordinate(longitude)) {
            errorCode = INVALID_COORDINATE;
            return false;
        }

        if (countryId == null || countryId.isEmpty() || stateId == null || stateId.isEmpty()) {
            errorCode = MISSING_FOREIGN_KEY;
            return false;
        }
        Places state = (Places) foreignKeyMap.get(stateId);
        if (state == null || !state.validateState(foreignKeyMap)) {
            errorCode = DANGLING_FOREIGN_KEY;
            return false;
        }

        Places country = (Places) foreignKeyMap.get(countryId);
        if (country == null || !country.validateCountry(foreignKeyMap)) {
            errorCode = DANGLING_FOREIGN_KEY;
            return false;
        }

        return true;
    }

    public boolean validateCountry(Map<String, Insertable> foreignKeyMap) {
        if (!isValidCoordinate(latitude) || !isValidCoordinate(longitude)) {
            errorCode = INVALID_COORDINATE;
            return false;
        }
        return true;
    }

    public boolean validateEarth(Map<String, Insertable> foreignKeyMap) {
        return true;
    }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public String getTableName() {
        return type == null ? "" : type.name();
    }

    @Override
    public void getForiegnKeyFields(Map<String, Insertable> foreignKeyMap) {
    }
}
