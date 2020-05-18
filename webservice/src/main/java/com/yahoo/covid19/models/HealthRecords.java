/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19.models;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Include(rootLevel = true, type = "healthRecords")
@Entity
@Table(name = "health_records")
public class HealthRecords {
    @Id
    private UUID id;

    @JoinColumn(name = "regionId")
    @ManyToOne
    private Place place;

    @Column(name = "regionId", insertable = false, updatable = false)
    private String placeId;

    private String label;
    private String wikiId;
    private Date referenceDate;
    private Double longitude;
    private Double latitude;
    private Long totalDeaths;
    private Long totalConfirmedCases;
    private Long totalRecoveredCases;
    private Long totalTestedCases;
    private Long numPositiveTests;
    private Long numDeaths;
    private Long numRecoveredCases;
    private Long diffNumPositiveTests;
    private Long diffNumDeaths;
    private Double avgWeeklyDeaths;
    private Double avgWeeklyConfirmedCases;
    private Double avgWeeklyRecoveredCases;
    private String dataSource;
}