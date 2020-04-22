/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database.gsonAdapters;

import static com.yahoo.covid19.database.JoinTableNames.valueOf;

import com.yahoo.covid19.database.JoinTableNames;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class TableNameAdapter extends TypeAdapter<JoinTableNames> {
    @Override
    public void write(JsonWriter jsonWriter, JoinTableNames joinTableNames) throws IOException {
        jsonWriter.beginArray();
        jsonWriter.value(joinTableNames.name());
        jsonWriter.endArray();
    }

    @Override
    public JoinTableNames read(JsonReader jsonReader) throws IOException {
        JoinTableNames highestRankTable = null;
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            String type = jsonReader.nextString();
            if(type == null) {
                continue;
            }
            try {
                JoinTableNames table = valueOf(type);
                if (table != null) {
                    highestRankTable = highestRankTable != null && highestRankTable.getRank() < table.getRank()
                            ? highestRankTable : table;
                }
            } catch (IllegalArgumentException e) {
                //Do Nothing
            }
        }
        jsonReader.endArray();
        return highestRankTable;
    }
}
