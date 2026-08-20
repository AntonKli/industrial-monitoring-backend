package com.example.industrialmonitoring.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class MqttIdempotencyMigrationIntegrationTest {

    private static final String SESSION_A = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SESSION_B = "550e8400-e29b-41d4-a716-446655440001";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("migration_test")
            .withUsername("test_user")
            .withPassword("test_password");

    private Flyway flyway;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .cleanDisabled(false)
                .load();
        flyway.clean();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
    }

    @Test
    void shouldMigrateEmptySchemaAndCreateAllConstraints() {
        flyway.migrate();

        List<String> constraints = jdbc.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE constraint_type = 'UNIQUE'
                  AND constraint_name LIKE 'uq_%_device_session_seq'
                ORDER BY constraint_name
                """, String.class);

        assertThat(constraints).containsExactly(
                "uq_event_device_session_seq",
                "uq_health_device_session_seq",
                "uq_telemetry_device_session_seq"
        );
    }

    @Test
    void shouldPreserveLegacyDuplicatesAndEnforceNewSessionIdentity() {
        migrateToVersionTwo();
        jdbc.update("""
                INSERT INTO telemetry_records
                    (device_id, gateway_timestamp, sequence_number, temperature_c, rpm)
                VALUES ('legacy-device', 1000, 7, 20.0, 1000),
                       ('legacy-device', 2000, 7, 21.0, 1100)
                """);

        flyway.migrate();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM telemetry_records WHERE device_id = 'legacy-device'",
                Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM telemetry_records WHERE session_id IS NULL",
                Long.class)).isEqualTo(2L);

        assertThat(insertTelemetry(SESSION_A, 8)).isOne();
        assertThat(insertTelemetry(SESSION_A, 8)).isZero();
        assertThat(insertTelemetry(SESSION_B, 8)).isOne();

        assertThat(insertEvent(SESSION_A, 8)).isOne();
        assertThat(insertEvent(SESSION_A, 8)).isZero();
        assertThat(insertHealth(SESSION_A, 8)).isOne();
        assertThat(insertHealth(SESSION_A, 8)).isZero();
    }

    private void migrateToVersionTwo() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target(MigrationVersion.fromVersion("2"))
                .load()
                .migrate();
    }

    private int insertTelemetry(String sessionId, long sequence) {
        return jdbc.update("""
                INSERT INTO telemetry_records
                    (device_id, session_id, gateway_timestamp, sequence_number, temperature_c, rpm)
                VALUES ('new-device', CAST(? AS UUID), 3000, ?, 22.0, 1200)
                ON CONFLICT (device_id, session_id, sequence_number) DO NOTHING
                """, sessionId, sequence);
    }

    private int insertEvent(String sessionId, long sequence) {
        return jdbc.update("""
                INSERT INTO event_records
                    (device_id, session_id, gateway_timestamp, sequence_number, event_type)
                VALUES ('new-device', CAST(? AS UUID), 3000, ?, 'STATUS')
                ON CONFLICT (device_id, session_id, sequence_number) DO NOTHING
                """, sessionId, sequence);
    }

    private int insertHealth(String sessionId, long sequence) {
        return jdbc.update("""
                INSERT INTO health_records
                    (device_id, session_id, gateway_timestamp, sequence_number, state)
                VALUES ('new-device', CAST(? AS UUID), 3000, ?, 1)
                ON CONFLICT (device_id, session_id, sequence_number) DO NOTHING
                """, sessionId, sequence);
    }
}
