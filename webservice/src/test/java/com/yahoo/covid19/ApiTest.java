/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.query;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinition;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinitions;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.document;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.include;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.spring.controllers.JsonApiController;
import org.junit.jupiter.api.Test;


import javax.ws.rs.core.MediaType;

/**
 * Example functional test.
 */
public class ApiTest extends IntegrationTest {

    /**
     * This test demonstrates returning DB metadata.
     */
    @Test
    public void jsonMetadataEndpoint() {
        when()
                .get("/api/json/v1/metadata?fields[metadata]=healthRecordsEndDate,healthRecordsStartDate")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * This test demonstrates an example test using the JSON-API DSL.
     */
    @Test
    void jsonApiGetTest1() {
        when()
                .get("/api/json/v1/healthRecords?fields[healthRecords]=label,latitude,longitude,referenceDate,wikiId,place"
                        + "&filter[healthRecords]=referenceDate=='2020-05-01T00:00Z';label=='Amador County, California'")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("healthRecords"),
                                        id("5390a416-a9d1-33a3-a040-f966a586db10"),
                                        attributes(
                                                attr("label", "Amador County, California"),
                                                attr("latitude", 38.4617),
                                                attr("longitude", -120.55011),
                                                attr("referenceDate", "2020-05-01T00:00Z"),
                                                attr("wikiId", "Amador_County,_California")
                                        ),
                                        relationships(
                                                relation("place", true,
                                                        linkage(type("places"), id("Amador_County,_California"))
                                                )
                                        )
                                )
                        ).toJSON())
                )
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * This test demonstrates an example test using the JSON-API DSL.
     */
    @Test
    void jsonApiGetTest2() {
        when()
                .get("/api/json/v1/healthRecords?fields[healthRecords]=label,latitude,longitude,referenceDate,wikiId,place"
                        + "&filter[healthRecords]=referenceDate=='2020-05-01T00:00Z';id=='0a5e3287-5fd4-391e-869c-9096491b5c46'")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("healthRecords"),
                                        id("0a5e3287-5fd4-391e-869c-9096491b5c46"),
                                        attributes(
                                                attr("label", "United States"),
                                                attr("latitude", 37.16793),
                                                attr("longitude", -95.84502),
                                                attr("referenceDate", "2020-05-01T00:00Z"),
                                                attr("wikiId", "United_States")
                                        ),
                                        relationships(
                                                relation("place", true,
                                                        linkage(type("places"), id("United_States"))
                                                )
                                        )
                                )
                        ).toJSON())
                )
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * This test demonstrates an example test using the JSON-API DSL.
     */
    @Test
    void jsonApiGetTest3() {
        when()
                .get("/api/json/v1/healthRecords?fields[healthRecords]=label,latitude,longitude,referenceDate,wikiId,place"
                        + "&filter[healthRecords]=referenceDate=='2020-05-02T00:00Z';label=='Washington, D.C.'")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("healthRecords"),
                                        id("deb0cab8-e9b7-3118-9432-ed26dcf11c0e"),
                                        attributes(
                                                attr("label", "Washington, D.C."),
                                                attr("latitude", 38.90476),
                                                attr("longitude", -77.01625),
                                                attr("referenceDate", "2020-05-02T00:00Z"),
                                                attr("wikiId", "Washington,_D.C.")
                                        ),
                                        relationships(
                                                relation("place", true,
                                                        linkage(type("places"), id("Washington,_D.C."))
                                                )
                                        )
                                )
                        ).toJSON())
                )
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void testEarthRecord() {
        when()
                .get("/api/json/v1/healthRecords?fields[healthRecords]=label,latitude,longitude,referenceDate,wikiId,place"
                        + "&filter=referenceDate=='2020-07-05T00:00Z';label=='Earth'")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("healthRecords"),
                                        id("806bc8e2-da10-331f-a924-fb250be5e408"),
                                        attributes(
                                                attr("label", "Earth"),
                                                attr("latitude", 0.0),
                                                attr("longitude", 0.0),
                                                attr("referenceDate", "2020-07-05T00:00Z"),
                                                attr("wikiId", "Earth")
                                        ),
                                        relationships(
                                                relation("place", true,
                                                        linkage(type("places"), id("Earth"))
                                                )
                                        )
                                )
                        ).toJSON())
                )
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void testCompoundFilterPredicate() {
        when()
                .get("/api/json/v1/healthRecords?fields[healthRecords]=label,latitude,longitude,referenceDate,wikiId,place"
                        + "&filter=referenceDate=='2020-07-05T00:00Z';place.parents=isempty=true")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("healthRecords"),
                                        id("806bc8e2-da10-331f-a924-fb250be5e408"),
                                        attributes(
                                                attr("label", "Earth"),
                                                attr("latitude", 0.0),
                                                attr("longitude", 0.0),
                                                attr("referenceDate", "2020-07-05T00:00Z"),
                                                attr("wikiId", "Earth")
                                        ),
                                        relationships(
                                                relation("place", true,
                                                        linkage(type("places"), id("Earth"))
                                                )
                                        )
                                )
                        ).toJSON())
                )
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void placeIncludeTest() {
        when()
                .get("/api/json/v1/healthRecords?filter[healthRecords]=referenceDate=='2020-05-01T00:00Z';id=='5390a416-a9d1-33a3-a040-f966a586db10'&include=place&fields[places]=longitude,latitude")
                .then()
                .log().all()
                .body(equalTo(
                        document(
                                data(
                                        resource(
                                                type("healthRecords"),
                                                id("5390a416-a9d1-33a3-a040-f966a586db10")
                                        )
                                ),
                                include(
                                        resource(
                                                type("places"),
                                                id("Amador_County,_California"),
                                                attributes(
                                                        attr("latitude", 38.4617),
                                                        attr("longitude", -120.55011)
                                                )
                                        )
                                )
                        ).toJSON())
                )
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void placeEarthTableTest() {
        when()
                .get("/api/json/v1/places?fields[places]=label,latitude,longitude,placeType,rank,wikiId,parents&filter=id=='Earth'")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("places"),
                                        id("Earth"),
                                        attributes(
                                                attr("label", "Earth"),
                                                attr("latitude", 0.0),
                                                attr("longitude", 0.0),
                                                attr("placeType", "AstronomicalObject"),
                                                attr("rank", 1),
                                                attr("wikiId", "Earth")
                                        ),
                                        relationships(
                                                relation("parents", false)
                                        )
                                )
                        ).toJSON())
                )
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void placeCountryTableTest() {
        when()
                .get("/api/json/v1/places?fields[places]=label,latitude,longitude,placeType,population,rank,wikiId,parents&filter=id=='United_States'")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("places"),
                                        id("United_States"),
                                        attributes(
                                                attr("label", "United States"),
                                                attr("latitude", 37.16793),
                                                attr("longitude", -95.84502),
                                                attr("placeType", "Country"),
                                                attr("population", 326687501),
                                                attr("rank", 2),
                                                attr("wikiId", "United_States")
                                        ),
                                        relationships(
                                                relation("parents", false,
                                                        linkage(type("places"), id("Earth"))
                                                )
                                        )
                                )
                        ).toJSON())
                )
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void placeStateTableTest() {
        when()
                .get("/api/json/v1/places?filter=id=='Washington,_D.C.'")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("places"),
                                        id("Washington,_D.C."),
                                        attributes(
                                                attr("label", "Washington, D.C."),
                                                attr("latitude", 38.90476),
                                                attr("longitude", -77.01625),
                                                attr("placeType", "StateAdminArea,CityTown"),
                                                attr("population", 705749),
                                                attr("rank", 3),
                                                attr("wikiId", "Washington,_D.C.")
                                        ),
                                        relationships(
                                                relation("children", false),
                                                relation("parents", false,
                                                        linkage(type("places"), id("United_States"))
                                                )
                                        )
                                )
                        ).toJSON())
                )
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void countyNonStaticRecords() {
        when()
                .get("/api/json/v1/latestHealthRecords")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void graphqlTest() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{ \"variables\" : {\"recordId\" : \"356779a9a1696714480f57fa3fb66d4c\"}, \"query\" : \"" + GraphQLDSL.document(
                        query( "myQuery",
                                variableDefinitions(
                                        variableDefinition("recordId", "[String]")
                                ),
                                selection(
                                        field("states",
                                                arguments(
                                                        argument("ids", "$recordId")
                                                ),
                                                selections(
                                                        field("id"),
                                                        field("label")
                                                )
                                        )
                                )
                        )
                        ).toQuery() + "\" }"
                )
                .when()
                .post("/api/graphql/v1")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void swaggerDocumentTest() {
        when()
                .get("/api/doc/v1")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void landingPageTest() {
        when()
                .get("/api")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void landingGraphiqlPage() {
        when()
                .get("/api/graphiql/index.html")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void landingSwaggerPage() {
        when()
                .get("/api/swagger/index.html")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void jsonApiDeleteIsRejected() {
        when()
                .delete("/api/json/v1/states/356779a9a1696714480f57fa3fb66d4c")
                .then()
                .statusCode(405);
    }

    @Test
    void jsonApiPatchIsRejected() {
        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("states"),
                                        id("356779a9a1696714480f57fa3fb66d4c"),
                                        attributes(
                                                attr("label", "2020-03-10")
                                        )
                                )
                        )
                )
                .when()
                .patch("/api/json/v1/states/356779a9a1696714480f57fa3fb66d4c")
                .then()
                .log().all()
                .statusCode(405);
    }

    @Test
    void jsonApiPostIsRejected() {
        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("healthRecords"),
                                        id("eb526f59-bdd2-3ff1-856c-67d46bd363a9"),
                                        attributes(
                                                attr("referenceDate", "2020-03-10")
                                        )
                                )
                        )
                )
                .when()
                .post("/api/json/v1/healthRecords")
                .then()
                .log().all()
                .statusCode(405);
    }

    @Test
    void jsonApiTestLargePageSizeRejected() {
        when()
                .get("/api/json/v1/healthRecords?page[size]=5000")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void jsonApiSmallPageSizeAccepted() {
        when()
                .get("/api/json/v1/healthRecords?page[size]=10")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void jsonApiStateQueryNotWhitelisted() {
        when()
                .get("/api/json/v1/states")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testRequestRejectedExceptionFilter() {
        when()
                .get("//%5a")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

}
