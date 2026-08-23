package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.HealthRecordEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class HealthRecordRepositoryIntegrationTest {

    private static final String SESSION_A = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SESSION_B = "550e8400-e29b-41d4-a716-446655440001";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("health_repository_test")
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
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldUseSequenceBeforeGatewayTimestampWithinGeneration() {
        insertV3(SESSION_A, 7, 100, 5000, "2026-08-20T10:00:00Z");
        insertV3(SESSION_A, 7, 90, 5000, "2026-08-20T11:00:00Z");

        assertThat(healthRecordRepository.findLatestObservedByDeviceId("edge01"))
                .get()
                .extracting(HealthRecordEntity::getSequenceNumber)
                .isEqualTo(100L);
    }

    @Test
    void shouldUseNewGenerationEvenWhenItsSequenceIsSmaller() {
        insertV3(SESSION_A, 7, 500, 500000, "2026-08-20T12:00:00Z");
        insertV3(SESSION_B, 8, 1, 1000, "2026-08-20T10:00:00Z");

        assertThat(healthRecordRepository.findLatestObservedByDeviceId("edge01"))
                .get()
                .satisfies(record -> {
                    assertThat(record.getSessionGeneration()).isEqualTo(8L);
                    assertThat(record.getSequenceNumber()).isEqualTo(1L);
                });
    }

    @Test
    void shouldExcludeRowsWithoutProtocolV3OrderingData() {
        jdbc.update("""
                INSERT INTO health_records (
                    device_id, session_id, gateway_timestamp, sequence_number, state
                )
                VALUES ('edge01', CAST(? AS UUID), 999000, 999, 1)
                """, SESSION_A);

        assertThat(healthRecordRepository.findLatestObservedByDeviceId("edge01")).isEmpty();
    }

    private void insertV3(
            String sessionId,
            long sessionGeneration,
            long sequenceNumber,
            long gatewayTimestamp,
            String createdAt
    ) {
        jdbc.update("""
                INSERT INTO health_records (
                    device_id, session_id, session_generation,
                    gateway_timestamp, sequence_number, state, created_at
                )
                VALUES ('edge01', CAST(? AS UUID), ?, ?, ?, 1, CAST(? AS TIMESTAMPTZ))
                """,
                sessionId,
                sessionGeneration,
                gatewayTimestamp,
                sequenceNumber,
                createdAt
        );
    }
}
