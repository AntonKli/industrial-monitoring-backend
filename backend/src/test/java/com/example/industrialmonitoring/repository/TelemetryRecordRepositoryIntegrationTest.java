package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
                telemetryRecordRepository.findByDeviceIdOrderByCreatedAtDesc("edge01");

        assertThat(result).hasSize(1);

        TelemetryRecordEntity savedRecord = result.getFirst();

        assertThat(savedRecord.getDeviceId()).isEqualTo("edge01");
        assertThat(savedRecord.getGatewayTimestamp()).isEqualTo(123000L);
        assertThat(savedRecord.getSequenceNumber()).isEqualTo(1L);
        assertThat(savedRecord.getTemperatureC()).isEqualByComparingTo("30.2");
        assertThat(savedRecord.getRpm()).isEqualTo(1600);
        assertThat(savedRecord.getCreatedAt()).isNotNull();
    }
}