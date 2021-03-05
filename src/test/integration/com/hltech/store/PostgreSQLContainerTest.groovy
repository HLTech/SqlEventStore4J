package com.hltech.store

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import spock.lang.Specification

import javax.sql.DataSource

@Slf4j
class PostgreSQLContainerTest extends Specification {

    static PostgreSQLContainer postgreSQLContainer
    static Sql dbClient
    static DataSource dataSource

    /**
     * Initialization block responsible for creating PostgreSQL container
     * Container is shared between test classes
     * It will be started at the start of given JVM and destroyed when JVM is stopped
     */
    static {
        createDb()
        setupDataSource()
        migrateDbScripts()
        createDbClient()
    }

    private static void createDb() {
        postgreSQLContainer = new PostgreSQLContainer("postgres:9.6")
        postgreSQLContainer.start()
        log.info("Container PostgreSQL is ready to use")
        log.info("JDBC: $postgreSQLContainer.jdbcUrl")
        log.info("Username: $postgreSQLContainer.username")
        log.info("Password: $postgreSQLContainer.password")
    }

    static setupDataSource() {
        dataSource = new PGSimpleDataSource()
        dataSource.setUser(postgreSQLContainer.getUsername())
        dataSource.setPassword(postgreSQLContainer.getPassword())
        dataSource.setUrl(postgreSQLContainer.getJdbcUrl())
    }

    private static void migrateDbScripts() {
        Flyway flyway = Flyway
                .configure()
                .dataSource(
                        postgreSQLContainer.getJdbcUrl(),
                        postgreSQLContainer.getUsername(),
                        postgreSQLContainer.getPassword())
                .load()
        flyway.migrate()
    }

    private static void createDbClient() {
        dbClient = Sql.newInstance(
                postgreSQLContainer.getJdbcUrl(),
                postgreSQLContainer.getUsername(),
                postgreSQLContainer.getPassword(),
                postgreSQLContainer.getDriverClassName()
        )
    }
}
