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

/**
 * Elide County Model.
 */
@Include(rootLevel = true, type = "counties")
@Entity
public class County extends Place {
    @JoinColumn(name = "stateId")
    @ManyToOne
    private State state;
}