/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.database;

import com.yahoo.covid19.database.models.LatestHealthRecords;
import com.yahoo.covid19.database.models.HistoricalHealthRecords;
import com.yahoo.covid19.database.models.Insertable;
import com.yahoo.covid19.database.models.Metadata;
import com.yahoo.covid19.database.models.Places;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Converts the github repo containing Yahoo Knowledge Graph data into a H2 database.
 */
@Slf4j
public class DatabaseBuilder {

    public static final SimpleDateFormat REFERENCE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    public static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        REFERENCE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        DB_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String JDBC_DRIVER = "org.h2.Driver";
    private static final String DATA_RESOURCE_FILE = "covid-19-data.tar.gz";

    private static final String CREATE_PLACE = "CREATE TABLE IF NOT EXISTS place\n" +
            "(\n" +
            "    id VARCHAR(255) DEFAULT NOT NULL,\n" +
            "    type VARCHAR(255) DEFAULT NULL,\n" +
            "    label VARCHAR(255) DEFAULT NULL,\n" +
            "    wikiId VARCHAR(255) DEFAULT NULL,\n" +
            "    longitude DOUBLE NOT NULL,\n" +
            "    latitude DOUBLE NOT NULL,\n" +
            "    population BIGINT DEFAULT NULL,\n" +
            "    PRIMARY KEY (id)\n" +
            ");\n" +
            "CREATE INDEX placeWikiIdx ON place (wikiId);\n" +
            "CREATE INDEX placeLabelIdx ON place (label);\n ";

    private static final String CREATE_RELATIONSHIPS = "CREATE TABLE IF NOT EXISTS relationship_hierarchy\n" +
            "(\n" +
            "    childId VARCHAR(255) DEFAULT NOT NULL,\n" +
            "    parentId VARCHAR(255) DEFAULT NULL,\n" +
            "    PRIMARY KEY (childId, parentId)\n" +
            ");\n";

    private static final String CREATE_HEALTH_RECORDS = "CREATE TABLE IF NOT EXISTS health_records\n" +
            "(\n" +
            "    id UUID NOT NULL,\n" +
            "    label VARCHAR(255) NOT NULL,\n" +
            "    referenceDate TIMESTAMP NOT NULL,\n" +
            "    regionId VARCHAR(255) DEFAULT NULL,\n" +
            "    longitude DOUBLE NOT NULL,\n" +
            "    latitude DOUBLE NOT NULL,\n" +
            "    wikiId VARCHAR(255) DEFAULT NULL,\n" +
            "    dataSource VARCHAR(255) DEFAULT NULL,\n" +
            "    totalDeaths BIGINT DEFAULT NULL,\n" +
            "    totalConfirmedCases BIGINT DEFAULT NULL,\n" +
            "    totalRecoveredCases BIGINT DEFAULT NULL,\n" +
            "    totalTestedCases BIGINT DEFAULT NULL,\n" +
            "    numPositiveTests BIGINT DEFAULT NULL,\n" +
            "    numDeaths BIGINT DEFAULT NULL,\n" +
            "    numRecoveredCases BIGINT DEFAULT NULL,\n" +
            "    diffNumPositiveTests BIGINT DEFAULT NULL,\n" +
            "    diffNumDeaths BIGINT DEFAULT NULL,\n" +
            "    avgWeeklyDeaths DOUBLE DEFAULT NULL,\n" +
            "    avgWeeklyConfirmedCases DOUBLE DEFAULT NULL,\n" +
            "    avgWeeklyRecoveredCases DOUBLE DEFAULT NULL,\n" +
            "    PRIMARY KEY (id)\n" +
            ");\n" +
            "CREATE INDEX healthRecordsRegionIdIdx ON health_records (regionId);\n" +
            "CREATE INDEX healthRecordsDateIdIdx ON health_records (referenceDate);";


    private static final String CREATE_LATEST_HEALTH_RECORDS = "CREATE TABLE IF NOT EXISTS latest_health_records\n" +
            "(\n" +
            "    id UUID NOT NULL,\n" +
            "    label VARCHAR(255) NOT NULL,\n" +
            "    referenceDate TIMESTAMP NOT NULL,\n" +
            "    regionId VARCHAR(255) DEFAULT NULL,\n" +
            "    longitude DOUBLE NOT NULL,\n" +
            "    latitude DOUBLE NOT NULL,\n" +
            "    wikiId VARCHAR(255) DEFAULT NULL,\n" +
            "    dataSource VARCHAR(255) DEFAULT NULL,\n" +
            "    totalDeaths BIGINT DEFAULT NULL,\n" +
            "    totalConfirmedCases BIGINT DEFAULT NULL,\n" +
            "    totalRecoveredCases BIGINT DEFAULT NULL,\n" +
            "    totalTestedCases BIGINT DEFAULT NULL,\n" +
            "    PRIMARY KEY (id)\n" +
            ");\n" +
            "CREATE INDEX latestHealthRecordsRegionIdIdx ON latest_health_records (regionId);";

    private static final String CREATE_METADATA = "CREATE TABLE IF NOT EXISTS metadata\n" +
            "(\n" +
            "    id VARCHAR(255) NOT NULL,\n" +
            "    healthRecordsStartDate TIMESTAMP NOT NULL,\n" +
            "    healthRecordsEndDate TIMESTAMP NOT NULL,\n" +
            "    publishedDate TIMESTAMP NOT NULL\n" +
            ");";

    private Map<String, Insertable> foreignKeyMap;

    public final static String PATH_SEPARATOR = System.getProperty("file.separator");
    private Gson gson;
    List<Insertable> insertables;
    List<Insertable> invalidInsertables = new ArrayList<>();
    private File outputDirectory;
    private long totalInputRecords = 0;
    private long totalDuplicateRecords = 0;
    private final double invalid_threshold;
    private final boolean failOnThresholdError;
    private Date lastModifiedDate = new Date();

    public class DBConnector implements Closeable {
        private Connection connection = null;

        public DBConnector() throws SQLException {
            close();
            String jdbcUrl = "jdbc:h2:" + DatabaseBuilder.this.outputDirectory + PATH_SEPARATOR + DBUtils.DB_NAME + ";DB_CLOSE_ON_EXIT=TRUE";

            try {
                Class.forName(JDBC_DRIVER);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException();
            }
            connection = DriverManager.getConnection(jdbcUrl, "", "");
        }

        public PreparedStatement getPreparedStatement(String sqlQueryStr) throws SQLException {
            if (connection == null) {
                throw new IllegalStateException("DB Connection error");
            }
            return connection.prepareStatement(sqlQueryStr);
        }

        public Object executeSQLQuery(String sqlQueryStr) throws SQLException {
            return executeSQLQuery(sqlQueryStr, x -> x);
        }

        public Object executeSQLQuery(String sqlQueryStr, Function<ResultSet, Object> function) throws SQLException {
            if (connection == null) {
                throw new IllegalStateException("DB Connection error");
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQueryStr)) {
                return executePreparedStatement(preparedStatement, function);
            }
        }

        public Object executePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
            return executePreparedStatement(preparedStatement, x -> x);
        }

        public Object executePreparedStatement(PreparedStatement preparedStatement, Function<ResultSet, Object> function) throws SQLException {
            log.info("Running SQL: {}", preparedStatement);
            boolean hasResult = preparedStatement.execute();

            if (hasResult) {
                try (ResultSet resultSet = preparedStatement.getResultSet()) {
                    return function.apply(resultSet);
                }
            }
            return null;
        }

        @Override
        public void close() {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }
    }

    public DatabaseBuilder(File outputDirectory, double invalid_threshold, boolean failOnThresholdError) {
        this.outputDirectory = outputDirectory;
        insertables = new ArrayList<>();
        gson = new GsonBuilder().registerTypeAdapter(Date.class, new GsonUTCDateAdapter()).create();
        this.invalid_threshold = invalid_threshold;
        this.failOnThresholdError = failOnThresholdError;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public void build() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Metadata metadata = new Metadata(insertables, lastModifiedDate);
        insertables.add(metadata);
        processInsertables();
        validateDbRecordCount();
    }

    public void processInputStream(String fileName, InputStream inputStream) {
        try {
            if (fileName.endsWith("region-metadata.json")) {
                log.info("Processing Data File: {}", fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                processInsertable(Places.class, reader);
            } else if (fileName.matches(".*/data/by-region-\\d{4}-\\d{2}-\\d{2}\\.json")) {
                log.info("Processing Data File: {}", fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                processInsertable(HistoricalHealthRecords.class, reader);
            } else if (fileName.endsWith("by-region-latest.json")) {
                log.info("Processing Data File: {}", fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                processInsertable(LatestHealthRecords.class, reader);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    public void processInsertable(Class<? extends Insertable> tableType, BufferedReader reader) throws IOException {
        String line;
        do {
            line = reader.readLine();
            if (line != null) {
                totalInputRecords += 1;
            }
            try {
                Insertable row = gson.fromJson(line, tableType);
                if (row == null) {
                    log.error("Invalid {} Row: {}", tableType.getName(), line);
                    continue;
                }
                insertables.add(row);

            } catch (JsonSyntaxException e) {
                log.error("Invalid {} Row: {} Reason: {}", tableType.getName(), line, e.getMessage());
                continue;
            }
        } while (line != null);
    }

    public void createTables(DBConnector connector) throws SQLException {
        connector.executeSQLQuery(CREATE_PLACE);
        connector.executeSQLQuery(CREATE_RELATIONSHIPS);
        connector.executeSQLQuery(CREATE_HEALTH_RECORDS);
        connector.executeSQLQuery(CREATE_LATEST_HEALTH_RECORDS);
        connector.executeSQLQuery(CREATE_METADATA);
    }

    public DBConnector newDBConnector() throws SQLException{
        return new DBConnector();
    }

    private void buildForeignKeyValidatorMap() {
        foreignKeyMap = insertables.stream()
                .filter(insertable -> insertable.getTableName().equals(Places.TABLE_NAME)
                        && insertable.getId() != null)
                .collect(Collectors.toMap(
                        insertable -> insertable.getId().toString(),
                        insertable -> insertable,
                        (insertable1, insertable2) -> {
                            log.error("found duplicate key: {}", insertable1.getId());
                            return insertable1;
                        }
                ));
    }

    public void processInsertables() {
        buildForeignKeyValidatorMap();

        try (DBConnector connector = newDBConnector()){
            HashSet<Object> idSet = new HashSet<>();
            List<Insertable> filteredInsertables = insertables.stream()
                    .filter(insertable -> {
                        boolean isValid = insertable.isValid(foreignKeyMap);
                        if (!isValid) {
                            invalidInsertables.add(insertable);
                            log.error("Invalid insert: {}", insertable.toString());
                            return false;
                        }

                        // don't allow duplicate insertable ids for a table
                        boolean wasAdded = idSet.add(insertable.getId() + insertable.getTableName());
                        if (!wasAdded) {
                            totalDuplicateRecords += 1;
                            log.error("Duplicate insertable key was found: {}", insertable.getId());
                            return false;
                        }

                        return true;
                    })
                    .map(insertable -> {
                        insertable.getForiegnKeyFields(foreignKeyMap);
                        return insertable;
                    })
                    .collect(Collectors.toList());

            createTables(connector);

            for (Insertable toInsert : filteredInsertables) {
                for (PreparedStatement insertStatement : toInsert.getStatements(connector)) {
                    try(PreparedStatement closableStatement = insertStatement) {
                        connector.executePreparedStatement(closableStatement);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void validateDbRecordCount() {
        double totalDbRecords = 0;
        double invalid_perc = 100;
        try (DBConnector dbConnector = newDBConnector()) {
            for (String tableName : Arrays.<String>asList("health_records","latest_health_records", "place")) {
                int rowCount = (int) dbConnector.executeSQLQuery("SELECT COUNT(*) FROM " + tableName + ";",
                        resultSet -> {
                            try {
                                return resultSet.last() ? resultSet.getInt(1) : 0;
                            } catch (Exception e) {
                                return 0;
                            }
                        });
                totalDbRecords += rowCount;
            }

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        if (totalInputRecords != 0) {
            invalid_perc = 100 - (totalDbRecords / totalInputRecords * 100);
        }

        invalidInsertables.stream()
                .collect(Collectors.groupingBy(
                        Insertable::getErrorCode,
                        Collectors.summingInt((ignored) -> new Integer(1))))
                .entrySet().stream().forEach((entry) -> {
            log.error(String.format("Invalid Record Reason: %s, Count: %d", entry.getKey(), entry.getValue()));
        });

        log.debug(String.format("Total input records read = %d", totalInputRecords));
        log.debug(String.format("Total Duplicate record processed = %d", totalDuplicateRecords));
        log.debug(String.format("Total valid record in db = %g", totalDbRecords));
        log.debug(String.format("Percentage of Invalid Records is %g%%", invalid_perc));

        if (failOnThresholdError &&
                (totalInputRecords == 0 || invalid_perc >= invalid_threshold)) {
            throw new IllegalStateException(
                    String.format(
                            "Percentage of Invalid Record %g did not meet the threshold %g",
                            invalid_perc,
                            invalid_threshold
                    )
            );
        }
    }

    public static void main(String[] args) {
        try {
            Options options = new Options();
            options.addOption("i", "archive-url", true,
                    "Location of the archived data (.tar.gz)."
                            + "github eg: https://git.ouroath.com/asafa/covid-19-data/tarball/master"
                            + "local eg: covid-19-data.tar.gz");
            options.addOption("o", "output-directory", true,
                    "Output directory to dump the database file");
            options.addOption("r", "download-data-from-repo", true,
                    "Flag that specifies that the data should be downloaded from git hub");
            options.addOption("u", "github-username", true,
                    "Github username in case if the repo is private");
            options.addOption("t", "github-accesstoken", true,
                    "Github access token in case if the repo is private");
            options.addOption("v", "schema-version", true,
                    "Database Schema Version");
            options.addOption("p", "invalid-perc-threshold", true,
                    "Precentage of invalid rows allowed in input");
            options.addOption("f", "fail-on-threshold-error", true,
                    "Flag to enable build failure on threshold error.");
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args);

            if (!commandLine.hasOption("archive-url")
                    || !commandLine.hasOption("output-directory")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("database uploader", options);
                return;
            }
            String githubTarballDownloadUrl = commandLine.getOptionValue("archive-url");
            String outputDirectory = commandLine.getOptionValue("output-directory");
            String downloadDataFromRepo = commandLine.getOptionValue("download-data-from-repo");
            String githubUsername = commandLine.getOptionValue("github-username");
            String githubAccessToken = commandLine.getOptionValue("github-accesstoken");
            double invalidThreshold = commandLine.hasOption("invalid-perc-threshold")
                    ? Double.valueOf(commandLine.getOptionValue("invalid-perc-threshold"))
                    : 1;
            Boolean failOnThresholdError = Boolean.parseBoolean(commandLine.getOptionValue("fail-on-threshold-error"));

            log.info("Processing data from {} and writing to {}", githubTarballDownloadUrl, outputDirectory);

            DatabaseBuilder databaseBuilder = new DatabaseBuilder(new File(outputDirectory), invalidThreshold, failOnThresholdError);
            DataFetcher dataFetcher;
            if ("true".equals(downloadDataFromRepo)) {
                log.info("Using data from github: {}", githubTarballDownloadUrl);
                dataFetcher = new GithubDataFetcher(githubTarballDownloadUrl, databaseBuilder,
                        githubUsername, githubAccessToken);
            } else {
                log.info("Using data from resource file: {}", DATA_RESOURCE_FILE);
                InputStream tarballFileStream = DatabaseBuilder.class.getClassLoader()
                        .getResourceAsStream(DATA_RESOURCE_FILE);
                dataFetcher = new DataFetcher(tarballFileStream, databaseBuilder);
            }
            dataFetcher.fetchDataAndProcess();
            log.info("Finished building.");
        } catch (ParseException e) {
            log.error("Parsing failed. Reason: ", e);
            throw new IllegalStateException("Parse Exception", e);
        } catch (IOException e) {
            log.error("IOException. Reason: ", e);
            throw new IllegalStateException("IOException", e);
        }
    }
}
