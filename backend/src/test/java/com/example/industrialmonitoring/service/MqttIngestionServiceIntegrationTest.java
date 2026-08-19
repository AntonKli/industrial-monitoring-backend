package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.EventMessage;
import com.example.industrialmonitoring.dto.HealthMessage;
import com.example.industrialmonitoring.dto.TelemetryMessage;
import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import com.example.industrialmonitoring.repository.DeviceRepository;
import com.example.industrialmonitoring.repository.EventRecordRepository;
import com.example.industrialmonitoring.repository.HealthRecordRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    @Autowired
    private EventRecordRepository eventRecordRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @BeforeEach
    void setUp() {
        telemetryRecordRepository.deleteAll();
        eventRecordRepository.deleteAll();
        healthRecordRepository.deleteAll();
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

    @Test
    void shouldAtomicallyRegisterOneDeviceAndPersistAllConcurrentRecords() throws Exception {
        int recordsPerType = 8;
        List<Callable<Void>> ingestions = new ArrayList<>();

        for (int i = 0; i < recordsPerType; i++) {
            long sequenceNumber = i;
            ingestions.add(() -> {
                mqttIngestionService.ingestTelemetry("concurrent-device", telemetryMessage(sequenceNumber));
                return null;
            });
            ingestions.add(() -> {
                mqttIngestionService.ingestEvent(
                        "concurrent-device",
                        new EventMessage(1, 123000L + sequenceNumber, sequenceNumber, "STATUS")
                );
                return null;
            });
            ingestions.add(() -> {
                mqttIngestionService.ingestHealth("concurrent-device", healthMessage(sequenceNumber));
                return null;
            });
        }

        runConcurrently(ingestions);

        assertThat(deviceRepository.count()).isEqualTo(1);
        assertThat(telemetryRecordRepository.findByDeviceIdOrderByCreatedAtDesc("concurrent-device"))
                .hasSize(recordsPerType);
        assertThat(eventRecordRepository.findByDeviceIdOrderByCreatedAtDesc("concurrent-device"))
                .hasSize(recordsPerType);
        assertThat(healthRecordRepository.findByDeviceIdOrderByCreatedAtDesc("concurrent-device"))
                .hasSize(recordsPerType);
    }

    @Test
    void shouldTreatExistingDeviceRegistrationAsNoOpAndPersistRecord() {
        mqttIngestionService.ingestTelemetry("existing-device", telemetryMessage(1));

        mqttIngestionService.ingestTelemetry("existing-device", telemetryMessage(2));

        assertThat(deviceRepository.count()).isEqualTo(1);
        assertThat(telemetryRecordRepository.findByDeviceIdOrderByCreatedAtDesc("existing-device"))
                .hasSize(2);
    }

    @Test
    void shouldRegisterAndPersistConcurrentMessagesForDifferentDevices() throws Exception {
        int deviceCount = 12;
        List<Callable<Void>> ingestions = new ArrayList<>();

        for (int i = 0; i < deviceCount; i++) {
            String deviceId = "parallel-device-" + i;
            long sequenceNumber = i;
            ingestions.add(() -> {
                mqttIngestionService.ingestTelemetry(deviceId, telemetryMessage(sequenceNumber));
                return null;
            });
        }

        runConcurrently(ingestions);

        assertThat(deviceRepository.count()).isEqualTo(deviceCount);
        assertThat(telemetryRecordRepository.count()).isEqualTo(deviceCount);
    }

    private TelemetryMessage telemetryMessage(long sequenceNumber) {
        return new TelemetryMessage(
                1,
                123000L + sequenceNumber,
                sequenceNumber,
                BigDecimal.valueOf(31.7),
                1750
        );
    }

    private HealthMessage healthMessage(long sequenceNumber) {
        return new HealthMessage(
                1,
                123000L + sequenceNumber,
                sequenceNumber,
                1,
                true,
                true,
                0,
                0L,
                3600L,
                0L,
                sequenceNumber,
                0L,
                0
        );
    }

    private void runConcurrently(List<Callable<Void>> tasks) throws Exception {
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Void>> synchronizedTasks = tasks.stream()
                .<Callable<Void>>map(task -> () -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                })
                .toList();

        try (ExecutorService executor = Executors.newFixedThreadPool(tasks.size())) {
            List<Future<Void>> futures = synchronizedTasks.stream()
                    .map(executor::submit)
                    .toList();

            ready.await();
            start.countDown();

            for (Future<Void> future : futures) {
                future.get();
            }
        }
    }
}
