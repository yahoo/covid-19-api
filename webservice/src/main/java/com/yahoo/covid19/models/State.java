/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19.models;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.Set;

@Include(rootLevel = true, type = "states")
@Entity
public class State extends Place {
    @OneToMany(mappedBy = "state")
    private Set<County> counties;

    @JoinColumn(name = "countryId")
    @ManyToOne
    private Country country;
}
