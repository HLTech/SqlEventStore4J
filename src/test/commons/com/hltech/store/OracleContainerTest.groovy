package com.hltech.store

import groovy.sql.Sql
import oracle.jdbc.pool.OracleDataSource
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.OracleContainer

import javax.sql.DataSource
import java.sql.Blob

trait OracleContainerTest {

    static Logger log = LoggerFactory.getLogger(OracleContainerTest.class)
    static OracleContainer oracleContainer
    static Sql dbClient
    static DataSource dataSource

    /**
     * Initialization block responsible for creating Oracle container
     * Container is shared between test classes
     * It will be started at the start of given JVM and destroyed when JVM is stopped
     */
    static {
        createDb()
        setupDataSource()
        migrateDbScripts()
        createDbClient()
    }

    static createDb() {
        oracleContainer = new OracleContainer("pvargacl/oracle-xe-18.4.0")
        oracleContainer.start()
        log.info("Container Oracle is ready to use")
        log.info("JDBC: $oracleContainer.jdbcUrl")
        log.info("Username: $oracleContainer.username")
        log.info("Password: $oracleContainer.password")
    }

    static setupDataSource() {
        dataSource = new OracleDataSource()
        dataSource.setUser(oracleContainer.getUsername())
        dataSource.setPassword(oracleContainer.getPassword())
        dataSource.setURL(oracleContainer.getJdbcUrl())
    }

    static String getTextFromBlob(Blob blobedPayload) {
        byte[] buffedPayload = blobedPayload.getBytes(1, (int) blobedPayload.length())
        String payload = new String(buffedPayload)
        payload
    }

    private static void migrateDbScripts() {
        Flyway flyway = Flyway
                .configure()
                .locations("db/migration/oracle")
                .baselineOnMigrate(true)
                .dataSource(
                        oracleContainer.getJdbcUrl(),
                        oracleContainer.getUsername(),
                        oracleContainer.getPassword())
                .load()
        flyway.migrate()
    }

    private static void createDbClient() {
        dbClient = Sql.newInstance(
                oracleContainer.getJdbcUrl(),
                oracleContainer.getUsername(),
                oracleContainer.getPassword(),
                oracleContainer.getDriverClassName()
        )
    }
}
