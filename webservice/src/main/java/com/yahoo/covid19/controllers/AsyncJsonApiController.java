/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */


package com.yahoo.covid19.controllers;

import com.yahoo.covid19.SecurityConfigProperties;
import com.yahoo.covid19.WhiteListProperties;
import com.yahoo.covid19.filters.URIQueryValidator;
import com.yahoo.elide.Elide;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.spring.controllers.JsonApiController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.yahoo.elide.spring.controllers.JsonApiController.JSON_API_CONTENT_TYPE;

import lombok.extern.slf4j.Slf4j;

/**
 * Overrides default Elide JSON-API controller to disable GET, PATCH, and DELETE and also enable request timeouts.
 */
@Configuration
@RequestMapping(value = "/api/json/v1")
@Slf4j
public class AsyncJsonApiController {

    JsonApiController controller;
    SecurityConfigProperties securityProperties;
    URIQueryValidator queryValidator;

    @Autowired
    public AsyncJsonApiController(Elide elide, ElideConfigProperties settings, SecurityConfigProperties properties) {
        controller = new JsonApiController(elide, settings);
        this.securityProperties = properties;
        this.queryValidator = new URIQueryValidator(properties.getWhiteList().getUri());
    }

    @GetMapping(value = "/**", produces = JSON_API_CONTENT_TYPE)
    public Callable<ResponseEntity<String>> elideGet(@RequestParam Map<String, String> allRequestParams,
                                                     HttpServletRequest request, Principal authentication) {
        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() {
                try {
                    // Skip validation if whitelist property is set to 'OFF'
                    if (!securityProperties.getWhiteList().isRuleState(WhiteListProperties.RuleState.OFF)) {
                        String queryURI = request.getRequestURI();
                        queryURI = (request.getQueryString() == null || request.getQueryString().isEmpty())
                                ? queryURI + "?"
                                : queryURI + "?" + URLDecoder.decode(request.getQueryString(), StandardCharsets.UTF_8.name());

                        // Error log if the query is not whitelisted. Log if rule is in ON or WARN state;
                        if (!queryValidator.validate(queryURI)) {
                            log.error(String.format("Query %s is not Whitelisted in API", queryURI));
                            if (securityProperties.getWhiteList().isRuleState(WhiteListProperties.RuleState.ON)) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                                        String.format("Query %s is not Whitelisted in API. %s", queryURI, securityProperties.getWhiteList().getRuleState().toString())
                                );
                            }
                        }

                    }
                    return controller.elideGet(allRequestParams, request, authentication);
                } catch (UnsupportedEncodingException ex) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Request Timeout");
                    }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error.");
                }
            }
        };
    }
}
