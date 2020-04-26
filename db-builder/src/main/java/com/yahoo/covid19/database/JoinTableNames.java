/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum JoinTableNames {
    Supername(0),
    Country(1),
    StateAdminArea(2),
    CountyAdminArea(3),
    CityTown(4)
    ;

    private int rank;

    JoinTableNames(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public static boolean contains(String value) {
        return !Arrays.asList(JoinTableNames.values()).stream()
                .filter(v -> v.name().equals(value))
                .collect(Collectors.toList())
                .isEmpty();
    }
}
