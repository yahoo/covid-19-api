/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.filter;

import com.yahoo.covid19.filters.URIQueryValidator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.List;


/***
 * Country Breakdown:
 * https://covid19.knowledge.yahoo.com/api/json/v1/healthRecords?fields[healthRecords]=totalConfirmedCases&fields[countries]=label&filter=referenceDate=in=('2020-04-03T00:00Z');county=isnull=true;state=isnull=true;country=isnull=false&sort=-totalConfirmedCases&include=country&page[offset]=0&page[limit]=4000
 *
 * State Breakdown:
 * https://covid19.knowledge.yahoo.com/api/json/v1/healthRecords?fields[healthRecords]=totalConfirmedCases&fields[states]=label&filter=country.id=='09d4bca31e2fd8b0f57f79f85ed42bd8';referenceDate=in=('2020-04-03T00:00Z');county=isnull=true;state=isnull=false&sort=-totalConfirmedCases&include=state&page[offset]=0&page[limit]=4000
 *
 * County Breakdown:
 * https://covid19.knowledge.yahoo.com/api/json/v1/healthRecords?fields[healthRecords]=totalConfirmedCases&fields[counties]=label&filter=state.id=='213fe69502445ed67ae8b99d22838802';referenceDate=in=('2020-04-03T00:00Z');county=isnull=false&sort=-totalConfirmedCases&include=county&page[offset]=0&page[limit]=4000
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class URIQueryValidatorTest {
    private List<String> strRules = Arrays.asList(
            "/api/json/v1/healthRecords?filter=referenceDate=={{DATE}}",
            "/api/json/v1/healthRecords?filter=referenceDate=in=({{DATE_LIST}})&includes=county",
            "/api/json/v1/county?filter=label=={{TYPE_AHEAD_CHARACTERS}}",
            "/api/json/v1/metadata?fields[metadata]={{ALPHA_NUMERIC}},{{ALPHA_NUMERIC}}",
            "/api/json/v1/states",
            "/api/json/v1/country?filter=wikiId=={{TYPE_AHEAD_CHARACTERS}}{{PAGINATION}}",
            "/api/json/v1/country?{{PAGINATION}}",
            "/api/json/v1/state?fields[state]={{FIELD_ATTRIBUTES}}{{PAGINATION}}",
            "/api/json/v1/country?cache={{[0|1]?}}",
            "/api/json/v1/country?cache={{[\\?)}}"             // Not a valid regex. Should be discarded.
    );
    private URIQueryValidator queryValidator;

    @BeforeAll
    public void init() {
        queryValidator = new URIQueryValidator(strRules);
    }

    @Test
    public void testRegex() {
        Assertions.assertEquals( "[" +
                "\\Q/api/json/v1/healthRecords?filter=referenceDate==\\E" + "'\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z'" + ", " +
                "\\Q/api/json/v1/healthRecords?filter=referenceDate=in=(\\E" + "(?:'\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z',?)+" + "\\Q)&includes=county\\E" + ", " +
                "\\Q/api/json/v1/county?filter=label==\\E" + "'[^\u0000-\u001F']+'" + ", " +
                "\\Q/api/json/v1/metadata?fields[metadata]=\\E" + "'[\\w\\-]+'" + "\\Q,\\E" + "'[\\w\\-]+'" + ", " +
                "\\Q/api/json/v1/states\\E" + ", " +
                "\\Q/api/json/v1/country?filter=wikiId==\\E" + "'[^\u0000-\u001F']+'" + "(?:&?page\\[\\w+\\]=\\d+)*" + ", " +
                "\\Q/api/json/v1/country?\\E" + "(?:&?page\\[\\w+\\]=\\d+)*" + ", " +
                "\\Q/api/json/v1/state?fields[state]=\\E" + "(?:(?!country|state|states|county|counties|&)[\\w,])*" + "(?:&?page\\[\\w+\\]=\\d+)*" + ", " +
                "\\Q/api/json/v1/country?cache=\\E" + "[0|1]?" +
                "]",
                queryValidator.getRegexRules().toString()
        );
    }

    @Test
    public void testURIWithDATE() {
        String uri1 = "/api/json/v1/healthRecords?filter=referenceDate=='2020-04-03T00:00Z'";
        Assertions.assertTrue(queryValidator.validate(uri1));

        ////Invalid Records

        // No single quotes around date
        String uri2 = "/api/json/v1/healthRecords?filter=referenceDate==2020-04-03T00:00Z";
        Assertions.assertFalse(queryValidator.validate(uri2));

        // Invalid date format yyyy-mm-dd
        String uri3 = "/api/json/v1/healthRecords?filter=referenceDate=='2020-4-3T00:00Z'";
        Assertions.assertFalse(queryValidator.validate(uri3));

        // Invalid date format yyyy-mm-dd
        String uri4 = "/api/json/v1/healthRecords?filter=referenceDate=='2020:04:03T00:00Z'";
        Assertions.assertFalse(queryValidator.validate(uri4));

        // Invalid time format Thh:mmZ
        String uri5 = "/api/json/v1/healthRecords?filter=referenceDate=='2020-04-03T00-00Z'";
        Assertions.assertFalse(queryValidator.validate(uri5));

        // Invalid time format Thh:mmZ
        String uri6 = "/api/json/v1/healthRecords?filter=referenceDate=='2020-04-03T8:00Z'";
        Assertions.assertFalse(queryValidator.validate(uri6));

        // Date and Time not delimited
        String uri7 = "/api/json/v1/healthRecords?filter=referenceDate=='2020-04-0300:00Z'";
        Assertions.assertFalse(queryValidator.validate(uri7));

        // Date and Time not delimited
        String uri8 = "/api/json/v1/healthRecords?filter=referenceDate=='2020-04-03T00:00'";
        Assertions.assertFalse(queryValidator.validate(uri8));

        // DateTime is enclose in brackets
        String uri9 = "/api/json/v1/healthRecords?filter=referenceDate==('2020-04-03T00:00')";
        Assertions.assertFalse(queryValidator.validate(uri9));

        // Root Country is not whitelisted uri
        String uri10 = "/api/json/v1/country?filter=referenceDate=='2020-04-03T00:00'";
        Assertions.assertFalse(queryValidator.validate(uri10));

        // Empty string
        String uri11 = "/api/json/v1/healthRecords?filter=referenceDate==''";
        Assertions.assertFalse(queryValidator.validate(uri11));
    }

    @Test
    public void testURIWithDATE_LIST() {
        String uri1 = "/api/json/v1/healthRecords?filter=referenceDate=in=('2020-04-03T00:00Z')&includes=county";
        Assertions.assertTrue(queryValidator.validate(uri1));

        String uri2 = "/api/json/v1/healthRecords?filter=referenceDate=in=('2020-04-03T00:00Z','2020-04-05T00:00Z')&includes=county";
        Assertions.assertTrue(queryValidator.validate(uri2));


        ////Invalid Records

        // Query does not have include=county predicate
        String uri3 = "/api/json/v1/healthRecords?filter=referenceDate=in=('2020-04-03T00:00Z','2020-04-05T00:00Z')";
        Assertions.assertFalse(queryValidator.validate(uri3));

        // include=country is expected
        String uri4 = "/api/json/v1/healthRecords?filter=referenceDate=in=('2020-04-03T00:00Z','2020-04-05T00:00Z')&include=";
        Assertions.assertFalse(queryValidator.validate(uri4));

        // No Single Quotes
        String uri5 = "/api/json/v1/healthRecords?filter=referenceDate=in=('2020-04-03T00:00Z,2020-04-05T00:00Z')&includes=county";
        Assertions.assertFalse(queryValidator.validate(uri5));

        // Invalid date format Thh:mmZ
        String uri6 = "/api/json/v1/healthRecords?filter=referenceDate=in=('2020:04:03T00:00Z','2020-04-05T00:00Z')&includes=county";
        Assertions.assertFalse(queryValidator.validate(uri6));

        // Invalid time format Thh:mmZ
        String uri7 = "/api/json/v1/healthRecords?filter=referenceDate=in=('2020-04-03T00:00Z','2020-04-05T00=00Z')&includes=county";
        Assertions.assertFalse(queryValidator.validate(uri7));

        // Date and Time not delimited
        String uri8 = "/api/json/v1/healthRecords?filter=referenceDate=in=('2020-04-03T00:00Z','2020-04-0500:00Z')&includes=county";
        Assertions.assertFalse(queryValidator.validate(uri8));

        // Date and Time not delimited
        String uri9 = "/api/json/v1/healthRecords?filter=referenceDate=in=('2020-04-03T00:00','2020-04-05T00:00Z')&includes=county";
        Assertions.assertFalse(queryValidator.validate(uri9));

        // DateTime is not enclose in brackets
        String uri10 = "/api/json/v1/healthRecords?filter=referenceDate=in='2020-04-03T00:00Z','2020-04-05T00:00Z'&includes=county";
        Assertions.assertFalse(queryValidator.validate(uri10));

        // Root Country is not whitelisted uri
        String uri11 = "/api/json/v1/state?filter=referenceDate=in=('2020-04-03T00:00Z','2020-04-05T00:00Z')&includes=county";
        Assertions.assertFalse(queryValidator.validate(uri11));

        // Empty list
        String uri12 = "/api/json/v1/healthRecords?filter=referenceDate=in=()&includes=county";
        Assertions.assertFalse(queryValidator.validate(uri12));
    }

    @Test
    public void testFilter() {
        String uri1 = "/api/json/v1/county?filter=label=='*In*'";
        Assertions.assertTrue(queryValidator.validate(uri1));

        String uri2 = "/api/json/v1/county?filter=label=='GO*'";
        Assertions.assertTrue(queryValidator.validate(uri2));

        String uri3 = "/api/json/v1/county?filter=label=='US'";
        Assertions.assertTrue(queryValidator.validate(uri3));

        String uri4 = "/api/json/v1/county?filter=label=='*Nation'";
        Assertions.assertTrue(queryValidator.validate(uri4));

        // Cannot conatin quotes
        String uri5 = "/api/json/v1/county?filter=label=='23'45'";
        Assertions.assertFalse(queryValidator.validate(uri5));

        // cannot be empty
        String uri6 = "/api/json/v1/county?filter=label==";
        Assertions.assertFalse(queryValidator.validate(uri6));

        // invalid unicode
        String uri7 = "/api/json/v1/county?filter=label=='wo\u0005rd'";
        Assertions.assertFalse(queryValidator.validate(uri7));

    }

    @Test
    public void testNoParameter() {
        String uri1 = "/api/json/v1/states";
        Assertions.assertTrue(queryValidator.validate(uri1));

        String uri2 = "/api/json/v1/states?filter=id==123";
        Assertions.assertFalse(queryValidator.validate(uri2));

    }

    @Test
    public void testUnicodeCharacters() {
        String uri1 = "/api/json/v1/country?filter=wikiId=='New_York\\,_(state)'&page[offset]=32";
        Assertions.assertTrue(queryValidator.validate(uri1));

        String uri2 = "/api/json/v1/country?filter=wikiId=='asd\u09fe\u0020'&page[offset]=32&page[limit]=10";
        Assertions.assertTrue(queryValidator.validate(uri2));

        String uri3 = "/api/json/v1/country?filter=wikiId=='こんにちは'&page[offset]=32";
        Assertions.assertTrue(queryValidator.validate(uri3));


        String uri4 = "/api/json/v1/country?filter=wikiId=='\u001f'";
        Assertions.assertFalse(queryValidator.validate(uri4));

        String uri5 = "/api/json/v1/country?filter=wikiId=='\u0034asf\u642f\u000023'";
        Assertions.assertFalse(queryValidator.validate(uri5));

        //Quotes are not allowed
        String uri6 = "/api/json/v1/country?filter=wikiId=='Label's'&page[offset]=32";
        Assertions.assertFalse(queryValidator.validate(uri6));
    }

    @Test
    public void testPagination() {
        String uri1 = "/api/json/v1/country?page[limit]=10";
        Assertions.assertTrue(queryValidator.validate(uri1));

        String uri2 = "/api/json/v1/country?page[number]=30&page[offset]=02";
        Assertions.assertTrue(queryValidator.validate(uri2));

        String uri3 = "/api/json/v1/country?";
        Assertions.assertTrue(queryValidator.validate(uri3));

        // page only takes number
        String uri4 = "/api/json/v1/country?page[size]=ten";
        Assertions.assertFalse(queryValidator.validate(uri4));

        // page does not have parameter
        String uri5 = "/api/json/v1/country?page=10";
        Assertions.assertFalse(queryValidator.validate(uri5));

    }

    @Test
    public void edgeCaseRegexFormation() {
        // Check if ALL regex works
        Assertions.assertEquals(
                "[\\Q/\\E.*, , \\Q/\\E]",
                (new URIQueryValidator(Arrays.asList("/{{ALL}}","","/"))).getRegexRules().toString()
        );

        // The below urirule should form a valid regex and should not break.
        // fields{metadata} - The regex contruction should not break if single '{' is encountered in uri or outside mecro definition
        // {{\w{6}\d}} - Regex Definition has '{' to specify quantifier, which conflicts demarker - valid regex
        // {{\w{6}}} - '}' quantifier is the end of regex - valid regex
        // {{\w{6}}}{{\w{5}\d}} - Two consicutive parameter definition.
        String uriRule = "/api/json/v1/metadata?fields{metadata}={{ALPHA_NUMERIC}},{{\\w{6}\\d}}&include={{\\w{6}}}{{\\w{5}\\d}}";
        String expectd = "[\\Q/api/json/v1/metadata?fields{metadata}=\\E"
                + "'[\\w\\-]+'" + "\\Q,\\E" + "\\w{6}\\d"
                + "\\Q&include=\\E" + "\\w{6}" + "\\w{5}\\d" + "]";
        Assertions.assertEquals(
                expectd,
                (new URIQueryValidator(Arrays.asList(uriRule))).getRegexRules().toString()
        );
    }

    @Test
    public void testFieldAttributes() {
        String uri1 = "/api/json/v1/state?fields[state]=id,label";
        Assertions.assertTrue(queryValidator.validate(uri1));

        String uri2 = "/api/json/v1/state?fields[state]=id,label,numCases,wikiId";
        Assertions.assertTrue(queryValidator.validate(uri2));

        //Space not expected
        String uri3 = "/api/json/v1/state?fields[state]=id, numCases,wikiId";
        Assertions.assertFalse(queryValidator.validate(uri3));

        //relationship not expected
        String uri4 = "/api/json/v1/state?fields[state]=id,wikiId,country";
        Assertions.assertFalse(queryValidator.validate(uri4));

        //With Page

        String uri5 = "/api/json/v1/state?fields[state]=id,label&page[limit]=20";
        Assertions.assertTrue(queryValidator.validate(uri5));

        String uri6 = "/api/json/v1/state?fields[state]=id,label,numCases,wikiId&page[limit]=20";
        Assertions.assertTrue(queryValidator.validate(uri6));

        //Space not expected
        String uri7 = "/api/json/v1/state?fields[state]=id,numCases,wikiId &page[limit]=20";
        Assertions.assertFalse(queryValidator.validate(uri7));

        //relationship not expected
        String uri8 = "/api/json/v1/state?fields[state]=id,wikiId,country&page[limit]=20";
        Assertions.assertFalse(queryValidator.validate(uri8));
    }

}
