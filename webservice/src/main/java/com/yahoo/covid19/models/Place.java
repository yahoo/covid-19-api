/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19.models;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Place {
    @Id
    private String id;
    private String label;
    private String wikiId;
    private Double longitude;
    private Double latitude;
    private Long population;
}