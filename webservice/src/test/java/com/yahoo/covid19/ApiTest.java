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
                        + "&filter[healthRecords]=referenceDate=='2020-05-01T00:00Z';id=='67986c35-74e1-3714-8707-6ffd0f6ca1b7'")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("healthRecords"),
                                        id("67986c35-74e1-3714-8707-6ffd0f6ca1b7"),
                                        attributes(
                                                attr("label", "Colombia"),
                                                attr("latitude", 4.11641),
                                                attr("longitude", -72.95853),
                                                attr("referenceDate", "2020-05-01T00:00Z"),
                                                attr("wikiId", "Colombia")
                                        ),
                                        relationships(
                                                relation("place", true,
                                                        linkage(type("places"), id("Colombia"))
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
                        + "&filter[healthRecords]=referenceDate=='2020-05-01T00:00Z';label=='Washington, D.C.'")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("healthRecords"),
                                        id("7c5cff5f-3181-32fc-a8ad-9cfd02b77cb1"),
                                        attributes(
                                                attr("label", "Washington, D.C."),
                                                attr("latitude", 38.90476),
                                                attr("longitude", -77.01625),
                                                attr("referenceDate", "2020-05-01T00:00Z"),
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
                        + "&filter=referenceDate=='2020-05-01T00:00Z';label=='Earth'")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("healthRecords"),
                                        id("311b5a8d-3f9d-3db0-aec6-a1adeac3c241"),
                                        attributes(
                                                attr("label", "Earth"),
                                                attr("latitude", 0.0),
                                                attr("longitude", 0.0),
                                                attr("referenceDate", "2020-05-01T00:00Z"),
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
                        + "&filter=referenceDate=='2020-05-01T00:00Z';place.parents=isempty=true")
                .then()
                .log().all()
                .body(equalTo(
                        data(
                                resource(
                                        type("healthRecords"),
                                        id("311b5a8d-3f9d-3db0-aec6-a1adeac3c241"),
                                        attributes(
                                                attr("label", "Earth"),
                                                attr("latitude", 0.0),
                                                attr("longitude", 0.0),
                                                attr("referenceDate", "2020-05-01T00:00Z"),
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
    void placeTableTest() {
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
