/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.controllers;

import com.yahoo.covid19.SecurityConfigProperties;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;

@Configuration
@RequestMapping(value = "/api/index.html")
@Slf4j
public class LandingPageController {

    boolean graphqlEnabled;
    boolean cspEnabled;
    boolean indexPagedEnabled;

    @Autowired
    public LandingPageController(ElideConfigProperties config, SecurityConfigProperties securityProperties) {
        graphqlEnabled = config.getGraphql().isEnabled();
        cspEnabled = securityProperties.isCspEnabled();
        indexPagedEnabled = securityProperties.isIndexPageEnabled();
    }

    @GetMapping(value = "", produces = "text/html")
    public ResponseEntity getLandingPage(@RequestParam Map<String, String> allRequestParams,
                           HttpServletRequest request,
                           Principal authentication) throws Exception {

        if (!indexPagedEnabled) {
            return ResponseEntity.notFound().build();
        }

        String templateBody;
        String cspHeader;
        if (graphqlEnabled) {
            log.error("Loading swagger & graphql instead");
            templateBody = TemplateEngine.getTemplate("templates/index.html");
            cspHeader = "default-src 'none'; script-src 'self'; font-src data:; connect-src 'self'; img-src 'self' data:; style-src 'self'; frame-src 'self'";
        } else {
            log.error("Loading swagger instead");
            templateBody = TemplateEngine.getTemplate("templates/swagger/index.html");
            cspHeader = "default-src 'none'; script-src 'self'; font-src data:; connect-src 'self'; img-src 'self' data:; style-src 'self'";
        }

        if (cspEnabled) {
            return ResponseEntity
                    .ok()
                    .header("Content-Security-Policy", cspHeader)
                    .body(templateBody);
        }

        return ResponseEntity.ok().body(templateBody);
    }
}
