/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database;


import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * Fetches the Yahoo Knowledge Graph github repo.
 */
@Slf4j
public class GithubDataFetcher extends DataFetcher {

    public GithubDataFetcher(String githubTarballUrl, DatabaseBuilder databaseBuilder,
                             String githubUsername, String githubAccessToken)
            throws IOException {
        super(getArchiveInputStream(githubTarballUrl, githubUsername, githubAccessToken), databaseBuilder);
    }

    private static InputStream getArchiveInputStream(String githubTarballUrl, String githubUsername, String githubAccessToken)
            throws IOException {
        HttpGet get = new HttpGet(githubTarballUrl);
        if (githubUsername != null && githubAccessToken != null) {
            String credential = githubUsername + ":" + githubAccessToken;
            String authorizationHeaderStringEncoded = "Basic " + Base64
                    .getEncoder().encodeToString(credential.getBytes());
            get.addHeader("Authorization", authorizationHeaderStringEncoded);
        }
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(get);
        return response.getEntity().getContent();
    }
}
