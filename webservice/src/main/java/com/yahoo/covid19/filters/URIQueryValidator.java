/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.filters;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
public class URIQueryValidator {

    private final List<String> regexRules;

    public enum RegexVariable {
        DATE("'?\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z'?"),
        DATE_LIST("(?:'\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z',?)+"),
        NUMBER("\\d+"),
        ALPHA_NUMERIC("'[\\w\\-]+'"),
        TYPE_AHEAD_CHARACTERS("'[^\u0000-\u001F]+'"),
        PAGINATION("(?:&?page\\[\\w+\\]=\\d+)*"),
        FIELD_ATTRIBUTES("(?:(?!place|places|&)[\\w,])*"),
        ALL(".*")
        ;
        private String regex;

        RegexVariable(String regex) {
            this.regex = regex;
        }

        public String getRegex() {
            return regex;
        }
    }

    /**
     * Builds regex for the list of rules
     * Regex Explanation
     * SampleURLRule = /api/json/v1/metadata?fields[metadata]={{ALPHA_NUMERIC}},{{ALPHA_NUMERIC}}
     * EdgeCaseRule = /api/json/v1/metadata?fields[metadata]={{\w{6}}},{{\w{6}\d}}
     * Regex Group 1 - ((?:(?!\{\{).)*)
     *                  - Matches all charaters from current position till '{{' - start of macro/regex demarker
     * Regex Group 2 - (\{\{(?:(?!\}\}).)+\}\}+)?
     *                  - Matches the macro/regex along with demarker. The match ends when '}}' is encountered
     *                  - It is important to include the demarker and remove that later. This allows to capture special case like {{\w{6}}}
     */
    public URIQueryValidator(List<String> strRules) {
        // Character '}' is escaped to make it more readable. (not necessary to be escaped in abset of '{')
        String pattern = "((?:(?!\\{\\{).)*)(\\{\\{(?:(?!\\}\\}).)+\\}\\}+)?";
        regexRules = new ArrayList<>();
        for(String strRule: strRules) {
            StringBuilder regexRule = new StringBuilder();
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(strRule.trim());
            while(m.find()) {
                regexRule.append(getAsStringLiteral(m.group(1)));
                regexRule.append(lookupParameter(m.group(2)));
            }
            String constructedRegex = regexRule.toString();
            try {
                Pattern.compile(constructedRegex);
                regexRules.add(constructedRegex);
            } catch (PatternSyntaxException e) {
                log.error(String.format(
                        "Error generating Regex.\nConfigured Rule: %s.\nConstructed Regex: %s",
                        strRule,
                        constructedRegex));
            }
        }
    }

    public List<String> getRegexRules() {
        return regexRules;
    }

    private String getAsStringLiteral(String literalMatch) {
        if (literalMatch == null || literalMatch.isEmpty()) {
            return "";
        }
        return Pattern.quote(literalMatch);
    }

    private String lookupParameter(String parameter) {
        if (parameter == null || parameter.isEmpty()) {
            return "";
        }
        String paramName = parameter.substring(2, parameter.length()-2);
        try {
            RegexVariable reg = RegexVariable.valueOf(paramName.toUpperCase());
            return (reg == null) ? paramName : reg.getRegex();
        } catch (IllegalArgumentException e) {
            return paramName;
        }
    }

    public boolean validate(String uri) {
        return regexRules.stream()
                .anyMatch(regexRule -> uri.matches(regexRule));
    }
}
