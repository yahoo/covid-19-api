/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19.models;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.Set;

@Include(rootLevel = true, type = "countries")
@Entity
public class Country extends Place {
    @OneToMany(mappedBy = "country")
    private Set<State> states;
}
