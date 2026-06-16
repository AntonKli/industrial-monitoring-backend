package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.HealthMessage;
import com.example.industrialmonitoring.entity.HealthRecordEntity;
import com.example.industrialmonitoring.repository.DeviceRepository;
import com.example.industrialmonitoring.repository.HealthRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class HealthIngestionServiceIntegrationTest {

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

        registry.add("mqtt.subscriber.enabled", () -> false);
        registry.add("mqtt.broker-url", () -> "tcp://localhost:1883");
        registry.add("mqtt.client-id", () -> "test-client-health-ingestion");
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
    private HealthRecordRepository healthRecordRepository;

    @BeforeEach
    void setUp() {
        healthRecordRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void shouldCreateDeviceAndPersistHealthRecord() {
        HealthMessage message = new HealthMessage(
                1,
                123002L,
                44L,
                2,
                true,
                true,
                0,
                0L,
                123L,
                1L,
                120L,
                0L,
                0
        );

        mqttIngestionService.ingestHealth("edge01", message);

        assertThat(deviceRepository.existsByDeviceId("edge01")).isTrue();

        List<HealthRecordEntity> records = healthRecordRepository.findAll();

        assertThat(records).hasSize(1);

        HealthRecordEntity savedRecord = records.getFirst();

        assertThat(savedRecord.getDeviceId()).isEqualTo("edge01");
        assertThat(savedRecord.getGatewayTimestamp()).isEqualTo(123002L);
        assertThat(savedRecord.getSequenceNumber()).isEqualTo(44L);
        assertThat(savedRecord.getState()).isEqualTo(2);
        assertThat(savedRecord.getMqttConnected()).isTrue();
        assertThat(savedRecord.getPubLastOk()).isTrue();
        assertThat(savedRecord.getBufferFill()).isEqualTo(0);
        assertThat(savedRecord.getBufferDrops()).isEqualTo(0L);
        assertThat(savedRecord.getDiagUptimeS()).isEqualTo(123L);
        assertThat(savedRecord.getDiagReconnects()).isEqualTo(1L);
        assertThat(savedRecord.getDiagPubOk()).isEqualTo(120L);
        assertThat(savedRecord.getDiagPubFail()).isEqualTo(0L);
        assertThat(savedRecord.getDiagLastError()).isEqualTo(0);
        assertThat(savedRecord.getCreatedAt()).isNotNull();
    }
}