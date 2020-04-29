/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19.models;

import com.yahoo.elide.annotation.Include;

import org.hibernate.annotations.Type;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Include(rootLevel = true, type = "latestHealthRecords")
@Entity
@Table(name = "latest_health_records")
public class LatestHealthRecords {
    @Id
    private UUID id;

    @JoinColumn(name = "regionId")
    @OneToOne
    private Place place;

    @Type(type = "com.yahoo.covid19.usertypes.StringCollection")
    private List<String> parentPlaces;

    private String label;
    private String wikiId;
    private Date referenceDate;
    private Double longitude;
    private Double latitude;
    private Long totalDeaths;
    private Long totalConfirmedCases;
    private Long totalRecoveredCases;
    private Long totalTestedCases;
    private Long numActiveCases;
    private Long numDeaths;
    private Long numRecoveredCases;
    private Long numTested;
    private String dataSource;
}
