/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
public class WhiteListProperties {
    private RuleState ruleState = RuleState.WARN;
    private List<String> uri = Arrays.asList();  //block all the query by default.

    public enum RuleState{
        ON,
        OFF,
        WARN
    }

    public boolean isRuleState(RuleState rule) {
        return ruleState.equals(rule);
    }
}
