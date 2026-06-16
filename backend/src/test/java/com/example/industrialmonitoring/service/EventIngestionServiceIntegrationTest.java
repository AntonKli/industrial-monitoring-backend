package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.EventMessage;
import com.example.industrialmonitoring.entity.EventRecordEntity;
import com.example.industrialmonitoring.repository.DeviceRepository;
import com.example.industrialmonitoring.repository.EventRecordRepository;
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
class EventIngestionServiceIntegrationTest {

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
        registry.add("mqtt.client-id", () -> "test-client-event-ingestion");
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
    private EventRecordRepository eventRecordRepository;

    @BeforeEach
    void setUp() {
        eventRecordRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void shouldCreateDeviceAndPersistEventRecord() {
        EventMessage message = new EventMessage(
                1,
                123001L,
                43L,
                "ALARM_RAISED"
        );

        mqttIngestionService.ingestEvent("edge01", message);

        assertThat(deviceRepository.existsByDeviceId("edge01")).isTrue();

        List<EventRecordEntity> records = eventRecordRepository.findAll();

        assertThat(records).hasSize(1);

        EventRecordEntity savedRecord = records.getFirst();

        assertThat(savedRecord.getDeviceId()).isEqualTo("edge01");
        assertThat(savedRecord.getGatewayTimestamp()).isEqualTo(123001L);
        assertThat(savedRecord.getSequenceNumber()).isEqualTo(43L);
        assertThat(savedRecord.getEventType()).isEqualTo("ALARM_RAISED");
        assertThat(savedRecord.getCreatedAt()).isNotNull();
    }
}