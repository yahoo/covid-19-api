/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.controllers;

import com.google.common.io.Resources;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TemplateEngine {
    public static String getTemplate(String path) throws IOException {
        URL url = Resources.getResource(path);
        return Resources.toString(url, StandardCharsets.UTF_8);
    }
}
