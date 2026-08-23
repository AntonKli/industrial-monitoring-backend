package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class TelemetryRecordRepositoryIntegrationTest {

    private static final String SESSION_A = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SESSION_B = "550e8400-e29b-41d4-a716-446655440001";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("industrial_monitoring_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private TelemetryRecordRepository telemetryRecordRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldSaveAndFindTelemetryRecordByDeviceId() {
        TelemetryRecordEntity telemetryRecord = new TelemetryRecordEntity(
                "edge01",
                123000L,
                1L,
                BigDecimal.valueOf(30.2),
                1600
        );

        telemetryRecordRepository.save(telemetryRecord);

        List<TelemetryRecordEntity> result =
                telemetryRecordRepository.findByDeviceIdOrderByCreatedAtDescIdDesc("edge01");

        assertThat(result).hasSize(1);

        TelemetryRecordEntity savedRecord = result.getFirst();

        assertThat(savedRecord.getDeviceId()).isEqualTo("edge01");
        assertThat(savedRecord.getGatewayTimestamp()).isEqualTo(123000L);
        assertThat(savedRecord.getSequenceNumber()).isEqualTo(1L);
        assertThat(savedRecord.getTemperatureC()).isEqualByComparingTo("30.2");
        assertThat(savedRecord.getRpm()).isEqualTo(1600);
        assertThat(savedRecord.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldOrderHistoryByCreatedAtThenId() {
        insertV3("edge01", SESSION_A, 7, 1, 1000, "2026-08-20T10:00:00Z");
        insertV3("edge01", SESSION_A, 7, 2, 2000, "2026-08-20T10:00:00Z");

        List<TelemetryRecordEntity> records =
                telemetryRecordRepository.findByDeviceIdOrderByCreatedAtDescIdDesc("edge01");

        assertThat(records).extracting(TelemetryRecordEntity::getSequenceNumber)
                .containsExactly(2L, 1L);
    }

    @Test
    void shouldUseSequenceBeforeGatewayTimestampWithinGeneration() {
        insertV3("edge01", SESSION_A, 7, 100, 5000, "2026-08-20T10:00:00Z");
        insertV3("edge01", SESSION_A, 7, 90, 5000, "2026-08-20T11:00:00Z");

        assertThat(telemetryRecordRepository.findLatestObservedByDeviceId("edge01"))
                .get()
                .extracting(TelemetryRecordEntity::getSequenceNumber)
                .isEqualTo(100L);
        assertThat(telemetryRecordRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldUseGenerationBeforeSequenceAndKeepDevicesSeparate() {
        insertV3("edge01", SESSION_A, 7, 500, 500000, "2026-08-20T12:00:00Z");
        insertV3("edge01", SESSION_B, 8, 1, 1000, "2026-08-20T10:00:00Z");
        insertV3("edge02", SESSION_A, 20, 2, 2000, "2026-08-20T11:00:00Z");

        assertThat(telemetryRecordRepository.findLatestObservedByDeviceId("edge01"))
                .get()
                .satisfies(record -> {
                    assertThat(record.getSessionGeneration()).isEqualTo(8L);
                    assertThat(record.getSequenceNumber()).isEqualTo(1L);
                });
        assertThat(telemetryRecordRepository.findLatestObservedByDeviceId("edge02"))
                .get()
                .extracting(TelemetryRecordEntity::getSessionGeneration)
                .isEqualTo(20L);
    }

    @Test
    void shouldExcludeProtocolV2RowsFromLatestObserved() {
        telemetryRecordRepository.save(new TelemetryRecordEntity(
                "edge01",
                999000L,
                999L,
                BigDecimal.valueOf(99.9),
                9999
        ));

        assertThat(telemetryRecordRepository.findLatestObservedByDeviceId("edge01")).isEmpty();
    }

    private void insertV3(
            String deviceId,
            String sessionId,
            long sessionGeneration,
            long sequenceNumber,
            long gatewayTimestamp,
            String createdAt
    ) {
        jdbc.update("""
                INSERT INTO telemetry_records (
                    device_id, session_id, session_generation,
                    gateway_timestamp, sequence_number,
                    temperature_c, rpm, created_at
                )
                VALUES (?, CAST(? AS UUID), ?, ?, ?, 30.2, 1600, CAST(? AS TIMESTAMPTZ))
                """,
                deviceId,
                sessionId,
                sessionGeneration,
                gatewayTimestamp,
                sequenceNumber,
                createdAt
        );
    }
}
