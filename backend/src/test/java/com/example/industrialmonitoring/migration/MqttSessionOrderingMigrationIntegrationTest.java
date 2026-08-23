package com.example.industrialmonitoring.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class MqttSessionOrderingMigrationIntegrationTest {

    private static final String SESSION_A = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SESSION_B = "550e8400-e29b-41d4-a716-446655440001";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("session_ordering_migration_test")
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
    void shouldMigrateEmptySchemaWithSessionRegistryConstraintsAndIndexes() {
        flyway.migrate();

        List<Map<String, Object>> columns = jdbc.queryForList("""
                SELECT table_name, data_type, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND column_name = 'session_generation'
                  AND table_name IN ('telemetry_records', 'event_records', 'health_records')
                ORDER BY table_name
                """);
        assertThat(columns).hasSize(3).allSatisfy(column -> {
            assertThat(column.get("data_type")).isEqualTo("bigint");
            assertThat(column.get("is_nullable")).isEqualTo("YES");
        });

        List<String> constraints = jdbc.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'mqtt_device_sessions'
                ORDER BY constraint_name
                """, String.class);
        assertThat(constraints).contains(
                "ck_mqtt_device_sessions_generation_positive",
                "fk_mqtt_device_sessions_device",
                "uq_mqtt_device_sessions_device_generation",
                "uq_mqtt_device_sessions_device_session"
        );

        String deleteRule = jdbc.queryForObject("""
                SELECT delete_rule
                FROM information_schema.referential_constraints
                WHERE constraint_name = 'fk_mqtt_device_sessions_device'
                """, String.class);
        assertThat(deleteRule).isEqualTo("RESTRICT");

        List<String> indexes = jdbc.queryForList("""
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                ORDER BY indexname
                """, String.class);
        assertThat(indexes).contains(
                "idx_telemetry_created_at_id",
                "idx_telemetry_device_created_at",
                "idx_telemetry_device_observed",
                "idx_events_created_at_id",
                "idx_events_device_created_at",
                "idx_health_created_at_id",
                "idx_health_device_created_at",
                "idx_health_device_observed"
        );

        String observedIndex = jdbc.queryForObject("""
                SELECT indexdef
                FROM pg_indexes
                WHERE indexname = 'idx_telemetry_device_observed'
                """, String.class);
        assertThat(observedIndex)
                .contains("session_generation DESC")
                .contains("sequence_number DESC")
                .contains("gateway_timestamp DESC")
                .contains("created_at DESC")
                .contains("id DESC")
                .contains("WHERE ((session_generation IS NOT NULL) AND (session_id IS NOT NULL))");
    }

    @Test
    void shouldMigrateFromVersionThreeWithoutBackfillOrLosingLegacyDuplicates() {
        migrateToVersionThree();
        jdbc.update("INSERT INTO devices (device_id) VALUES ('legacy-device')");
        jdbc.update("""
                INSERT INTO telemetry_records
                    (device_id, gateway_timestamp, sequence_number, temperature_c, rpm)
                VALUES ('legacy-device', 1000, 7, 20.0, 1000),
                       ('legacy-device', 2000, 7, 21.0, 1100)
                """);

        flyway.migrate();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM telemetry_records WHERE device_id = 'legacy-device'",
                Long.class
        )).isEqualTo(2L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM telemetry_records WHERE session_generation IS NULL",
                Long.class
        )).isEqualTo(2L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM mqtt_device_sessions",
                Long.class
        )).isZero();
    }

    @Test
    void shouldEnforcePositiveGenerationsAndBidirectionalDeviceSessionMapping() {
        flyway.migrate();
        jdbc.update("INSERT INTO devices (device_id) VALUES ('edge01')");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO telemetry_records
                    (device_id, session_id, session_generation, gateway_timestamp, sequence_number)
                VALUES ('edge01', CAST(? AS UUID), 0, 1000, 1)
                """, SESSION_A)).isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("""
                INSERT INTO mqtt_device_sessions
                    (device_id, session_generation, session_id)
                VALUES ('edge01', 7, CAST(? AS UUID))
                """, SESSION_A);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO mqtt_device_sessions
                    (device_id, session_generation, session_id)
                VALUES ('edge01', 7, CAST(? AS UUID))
                """, SESSION_B)).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO mqtt_device_sessions
                    (device_id, session_generation, session_id)
                VALUES ('edge01', 8, CAST(? AS UUID))
                """, SESSION_A)).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM mqtt_device_sessions",
                Long.class
        )).isOne();
    }

    private void migrateToVersionThree() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target(MigrationVersion.fromVersion("3"))
                .load()
                .migrate();
    }
}
