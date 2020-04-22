/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database;

import com.yahoo.covid19.database.models.Places;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

public class PlacesTest {

    @ParameterizedTest
    @CsvSource({"-120.66384, 40.44611, true", "1.2E10, 40.44611, true",  "40.44611, 1.2E10, true", "40, 1.ab0, false"})
    public void testLongitudeParsing(String longitude, String latitude, Boolean expected) {
        Places places = new Places("123456", "", "", longitude, latitude, "0", null, null, JoinTableNames.Country);
        Assertions.assertEquals(expected, places.isValid(null));
    }
}
