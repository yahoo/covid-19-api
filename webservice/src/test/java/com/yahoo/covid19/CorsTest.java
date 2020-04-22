/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static com.jayway.restassured.RestAssured.given;

public class CorsTest extends IntegrationTest {
    @Test
    public void successTest() {

        String origin = "https://www.sithlords.com";
        String requestHeaders = "origin,content-type,accept";

        /*
         * Web service allows access from any origin
         */
        given()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", requestHeaders)
                .when()
                .options("/data/v1/countyData")
                .then()
                .assertThat()
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "GET,OPTIONS")
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void failTest() {

        String origin = "https://www.sithlords.com";
        String requestHeaders = "origin,content-type,accept";

        /*
         * Web service allows access from any origin
         */
        given()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", requestHeaders)
                .when()
                .options("/data/v1/countyData")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }
}
