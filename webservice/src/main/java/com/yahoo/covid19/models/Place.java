/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.models;

import com.yahoo.elide.annotation.Include;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

@Include(rootLevel = true, type = "places")
@Entity
public class Place {
    @Id
    private String id;
    private String label;
    private String wikiId;
    private Double longitude;
    private Double latitude;
    private Long population;

    @ManyToMany
    @JoinTable(
            name = "relationship_hierarchy",
            joinColumns = @JoinColumn(name = "childId"),
            inverseJoinColumns = @JoinColumn(name = "parentId")
    )
    private Set<Place> parents;

    @ManyToMany
    @JoinTable(
            name = "relationship_hierarchy",
            joinColumns = @JoinColumn(name = "parentId"),
            inverseJoinColumns = @JoinColumn(name = "childId")
    )
    private Set<Place> children;
}
