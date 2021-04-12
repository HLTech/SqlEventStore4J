package testing.prerequisites;

import com.hltech.store.DummyBaseEvent;
import com.hltech.store.DummyEventBodyMapper;
import com.hltech.store.PostgresEventStore;
import com.hltech.store.versioning.DummyVersioningStrategy;
import groovy.sql.Sql;
import org.flywaydb.core.Flyway;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.postgresql.ds.PGPoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.SQLException;

@State(Scope.Benchmark)
public class PostgresEventStorePerfTestsPreparation {

    static Logger log = LoggerFactory.getLogger(PostgresEventStorePerfTestsPreparation.class);
    private PostgreSQLContainer postgreSQLContainer;
    private Sql dbClient;
    private PGPoolingDataSource dataSource;
    PostgresEventStore<DummyBaseEvent> eventStore;

    public PostgresEventStorePerfTestsPreparation() {
    }

    public PostgresEventStore<DummyBaseEvent> getEventStore() {
        return eventStore;
    }

    @Setup(Level.Trial)
    public void setupDb() throws SQLException, ClassNotFoundException {
        postgreSQLContainer = new PostgreSQLContainer("postgres:9.6");
        postgreSQLContainer.start();
        log.info("Container PostgreSQL is ready to use");
        log.info("JDBC: $postgreSQLContainer.jdbcUrl");
        log.info("Username: $postgreSQLContainer.username");
        log.info("Password: $postgreSQLContainer.password");
        setupDataSource();
        migrateDbScripts();
        createDbClient();
        setupEventStore();
    }

    private void setupEventStore() {
        this.eventStore = new PostgresEventStore(
                DummyBaseEvent.EVENT_ID_EXTRACTOR,
                DummyBaseEvent.AGGREGATE_ID_EXTRACTOR,
                new DummyVersioningStrategy(),
                new DummyEventBodyMapper(),
                dataSource
        );
    }

    private void setupDataSource() {
        dataSource = new PGPoolingDataSource();
        dataSource.setUser(postgreSQLContainer.getUsername());
        dataSource.setPassword(postgreSQLContainer.getPassword());
        dataSource.setUrl(postgreSQLContainer.getJdbcUrl());
    }

    private void migrateDbScripts() {
        Flyway flyway = Flyway
                .configure()
                .locations("db/migration/postgres")
                .dataSource(
                        postgreSQLContainer.getJdbcUrl(),
                        postgreSQLContainer.getUsername(),
                        postgreSQLContainer.getPassword())
                .load();
        flyway.migrate();
    }

    private void createDbClient() throws SQLException, ClassNotFoundException {
        dbClient = Sql.newInstance(
                postgreSQLContainer.getJdbcUrl(),
                postgreSQLContainer.getUsername(),
                postgreSQLContainer.getPassword(),
                postgreSQLContainer.getDriverClassName()
        );
    }
}
