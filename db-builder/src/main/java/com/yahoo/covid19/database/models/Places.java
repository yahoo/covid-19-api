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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.yahoo.covid19.database.ErrorCodes.DANGLING_FOREIGN_KEY;
import static com.yahoo.covid19.database.ErrorCodes.INVALID_COORDINATE;
import static com.yahoo.covid19.database.ErrorCodes.INVALID_ID;
import static com.yahoo.covid19.database.ErrorCodes.INVALID_TYPE;
import static com.yahoo.covid19.database.ErrorCodes.MISSING_FOREIGN_KEY;
import static com.yahoo.covid19.database.ErrorCodes.OK;

import com.google.gson.annotations.SerializedName;

/**
 * Used to slurp County records from Yahoo Knowledge Graph.
 */
@Slf4j
@Data
public class Places implements Insertable {
    public static final String TABLE_NAME = "places";
    private static final String SUPERNAME = "Supername";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String PLACE_INSERT_STATEMENT = "INSERT INTO place ("
                + "id, type, label, wikiId, longitude, "
                + "latitude, population) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?);";

    private static final String RELATIONSHIP_INSERT_STATEMENT = "INSERT INTO relationship_hierarchy ("
            + "childId, parentId) "
            + "VALUES (?, ?);";

    private final String id;
    private final List<String> type;
    private final String label;
    private final String wikiId;
    private final String longitude;
    private final String latitude;
    private final String population;
    @SerializedName(value = "parentId")
    private final List<String> parentIds;

    private transient ErrorCodes errorCode = OK;


    private PreparedStatement getCommonStatement(DatabaseBuilder.DBConnector connector) throws SQLException {
        PreparedStatement statement = connector.getPreparedStatement(PLACE_INSERT_STATEMENT);
        statement.setString(1, id);
        statement.setString(2, String.join(",", type));
        statement.setString(3, label);
        statement.setString(4, wikiId);
        statement.setDouble(5, (longitude == null) ? 0 : Double.valueOf(longitude));
        statement.setDouble(6, (latitude == null) ? 0 : Double.valueOf(latitude));
        statement.setObject(7, population == null ? null : Long.valueOf(population));
        return statement;
    }

    private PreparedStatement getRelationshipStatement(DatabaseBuilder.DBConnector connector, String parentId) throws SQLException {


        PreparedStatement statement = connector.getPreparedStatement(RELATIONSHIP_INSERT_STATEMENT);
        statement.setString(1, id);
        statement.setString(2, parentId);
        return statement;
    }

    @Override
    public List<PreparedStatement> getStatement(DatabaseBuilder.DBConnector connector) throws SQLException {
        List<PreparedStatement> statements = new ArrayList<>();
        statements.add(getCommonStatement(connector));

        for (String parentId : parentIds) {
            statements.add(getRelationshipStatement(connector, parentId));
        }

        return statements;
    }

    @Override
    public boolean isValid(Map<String, Insertable> foreignKeyMap) {
        if (id == null || id.isEmpty()) {
            errorCode = INVALID_ID;
            return false;
        }

        if (this.type == null || this.type.isEmpty()) {
            errorCode = INVALID_TYPE;
            return false;
        }

        if (this.type.contains(SUPERNAME)) {
            return true;
        }

        if (! isValidCoordinate(latitude) || ! isValidCoordinate(longitude)) {
            errorCode = INVALID_COORDINATE;
            return false;
        }

        if (parentIds == null || parentIds.isEmpty()) {
            errorCode = MISSING_FOREIGN_KEY;
            return false;
        }
        boolean isParentValid =  parentIds.stream()
                .map(parentId -> foreignKeyMap.get(parentId))
                .allMatch(
                        insertable -> {
                            if (insertable == null || !insertable.isValid(foreignKeyMap)) {
                                return false;
                            }
                            return true;
                        }
                );
        if (!isParentValid) {
            this.errorCode = DANGLING_FOREIGN_KEY;
            return false;
        }
        return true;
    }

    private boolean isValidCoordinate(String coordinate) {
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


    @Override
    public Object getId() {
        return id;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public void getForiegnKeyFields(Map<String, Insertable> foreignKeyMap) {
    }
}
