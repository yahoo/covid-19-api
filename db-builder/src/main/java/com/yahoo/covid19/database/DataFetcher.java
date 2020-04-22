/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.GZIPInputStream;

/**
 * Fetches the Yahoo Knowledge Graph data
 */
@Slf4j
public class DataFetcher {

    protected final DatabaseBuilder databaseBuilder;
    protected final InputStream archiveInputStream;
    private Date lastModifiedDate;

    public DataFetcher(InputStream archiveInputStream, DatabaseBuilder databaseBuilder) {
        this.databaseBuilder = databaseBuilder;
        this.archiveInputStream = archiveInputStream;
        this.lastModifiedDate = new Date(0);    // Initialize to Jan 01, 1970;
    }

    public void fetchDataAndProcess() {
        log.info("Started processing data from the repository");
        try(TarArchiveInputStream tarInputStream = new TarArchiveInputStream(
                    new GZIPInputStream(this.archiveInputStream))) {
            TarArchiveEntry tarArchiveEntry = tarInputStream.getNextTarEntry();
            while (tarArchiveEntry != null) {
                databaseBuilder.processInputStream(tarArchiveEntry.getName(), tarInputStream);
                Date fileDate = tarArchiveEntry.getLastModifiedDate();
                lastModifiedDate = lastModifiedDate.before(fileDate) ? fileDate : lastModifiedDate;
                tarArchiveEntry = tarInputStream.getNextTarEntry();
            }
            databaseBuilder.setLastModifiedDate(lastModifiedDate);
            databaseBuilder.build();
            log.info("Completed processing data from the repository");
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
