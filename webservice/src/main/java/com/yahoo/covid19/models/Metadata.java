/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.covid19.models;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Include(rootLevel = true, type = "metadata")
@Entity
@Table(name = "metadata")
public class Metadata {
    @Id
    private String id;

    public Date healthRecordsEndDate;
    public Date healthRecordsStartDate;
    public Date publishedDate;
}
