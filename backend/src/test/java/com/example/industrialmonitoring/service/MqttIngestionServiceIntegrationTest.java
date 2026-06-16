package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.TelemetryMessage;
import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import com.example.industrialmonitoring.repository.DeviceRepository;
import com.example.industrialmonitoring.repository.TelemetryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class MqttIngestionServiceIntegrationTest {

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

        registry.add("mqtt.broker-url", () -> "tcp://localhost:1883");
        registry.add("mqtt.client-id", () -> "test-client-ingestion");
        registry.add("mqtt.topic-root", () -> "rtz");
        registry.add("mqtt.device-id", () -> "edge01");
        registry.add("mqtt.username", () -> "edge");
        registry.add("mqtt.password", () -> "edge_password");
    }

    @Autowired
    private MqttIngestionService mqttIngestionService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TelemetryRecordRepository telemetryRecordRepository;

    @BeforeEach
    void setUp() {
        telemetryRecordRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void shouldCreateDeviceAndPersistTelemetryRecord() {
        TelemetryMessage message = new TelemetryMessage(
                1,
                123000L,
                42L,
                BigDecimal.valueOf(31.7),
                1750
        );

        mqttIngestionService.ingestTelemetry("edge01", message);

        assertThat(deviceRepository.existsByDeviceId("edge01")).isTrue();

        List<TelemetryRecordEntity> records =
                telemetryRecordRepository.findByDeviceIdOrderByCreatedAtDesc("edge01");

        assertThat(records).hasSize(1);

        TelemetryRecordEntity savedRecord = records.getFirst();

        assertThat(savedRecord.getDeviceId()).isEqualTo("edge01");
        assertThat(savedRecord.getGatewayTimestamp()).isEqualTo(123000L);
        assertThat(savedRecord.getSequenceNumber()).isEqualTo(42L);
        assertThat(savedRecord.getTemperatureC()).isEqualByComparingTo("31.7");
        assertThat(savedRecord.getRpm()).isEqualTo(1750);
        assertThat(savedRecord.getCreatedAt()).isNotNull();
    }
}