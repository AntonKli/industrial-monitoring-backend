package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.EventMessage;
import com.example.industrialmonitoring.dto.HealthMessage;
import com.example.industrialmonitoring.dto.TelemetryMessage;
import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import com.example.industrialmonitoring.repository.DeviceRepository;
import com.example.industrialmonitoring.repository.EventRecordRepository;
import com.example.industrialmonitoring.repository.HealthRecordRepository;
import com.example.industrialmonitoring.repository.TelemetryRecordRepository;
import io.micrometer.core.instrument.MeterRegistry;
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

    private static final String SESSION_A = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SESSION_B = "550e8400-e29b-41d4-a716-446655440001";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("industrial_monitoring_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("mqtt.subscriber.enabled", () -> false);
    }

    @Autowired
    private MqttIngestionService ingestionService;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private TelemetryRecordRepository telemetryRepository;
    @Autowired
    private EventRecordRepository eventRepository;
    @Autowired
    private HealthRecordRepository healthRepository;
    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        telemetryRepository.deleteAll();
        eventRepository.deleteAll();
        healthRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void shouldStoreThenDeduplicateTelemetryAndKeepFirstPayload() {
        IngestionResult first = ingestionService.ingestTelemetry(
                "edge01", telemetry(SESSION_A, 42, "31.7", 1750));
        IngestionResult second = ingestionService.ingestTelemetry(
                "edge01", telemetry(SESSION_A, 42, "99.9", 9999));

        assertThat(first).isEqualTo(IngestionResult.STORED);
        assertThat(second).isEqualTo(IngestionResult.DUPLICATE);
        List<TelemetryRecordEntity> records = telemetryRepository.findAll();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getTemperatureC()).isEqualByComparingTo("31.7");
        assertThat(records.getFirst().getRpm()).isEqualTo(1750);
    }

    @Test
    void shouldStoreThenDeduplicateEvent() {
        assertThat(ingestionService.ingestEvent("edge01", event(SESSION_A, 7, "ALARM_RAISED")))
                .isEqualTo(IngestionResult.STORED);
        assertThat(ingestionService.ingestEvent("edge01", event(SESSION_A, 7, "ALARM_CLEARED")))
                .isEqualTo(IngestionResult.DUPLICATE);
        assertThat(eventRepository.findAll()).singleElement()
                .extracting(record -> record.getEventType()).isEqualTo("ALARM_RAISED");
    }

    @Test
    void shouldStoreThenDeduplicateHealth() {
        assertThat(ingestionService.ingestHealth("edge01", health(SESSION_A, 8, 1)))
                .isEqualTo(IngestionResult.STORED);
        assertThat(ingestionService.ingestHealth("edge01", health(SESSION_A, 8, 2)))
                .isEqualTo(IngestionResult.DUPLICATE);
        assertThat(healthRepository.findAll()).singleElement()
                .extracting(record -> record.getState()).isEqualTo(1);
    }

    @Test
    void shouldAllowSameSequenceForDifferentSessions() {
        ingestionService.ingestTelemetry("edge01", telemetry(SESSION_A, 1, "31.7", 1750));
        ingestionService.ingestTelemetry("edge01", telemetry(SESSION_B, 1, "31.7", 1750));
        assertThat(telemetryRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldAllowSameSessionAndSequenceForDifferentDevices() {
        ingestionService.ingestTelemetry("edge01", telemetry(SESSION_A, 1, "31.7", 1750));
        ingestionService.ingestTelemetry("edge02", telemetry(SESSION_A, 1, "31.7", 1750));
        assertThat(telemetryRepository.count()).isEqualTo(2);
        assertThat(deviceRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldAllowDifferentSequencesInSameSession() {
        ingestionService.ingestTelemetry("edge01", telemetry(SESSION_A, 1, "31.7", 1750));
        ingestionService.ingestTelemetry("edge01", telemetry(SESSION_A, 2, "31.7", 1750));
        assertThat(telemetryRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldScopeIdentityByRecordTable() {
        ingestionService.ingestTelemetry("edge01", telemetry(SESSION_A, 5, "31.7", 1750));
        ingestionService.ingestEvent("edge01", event(SESSION_A, 5, "STATUS"));
        ingestionService.ingestHealth("edge01", health(SESSION_A, 5, 1));

        assertThat(telemetryRepository.count()).isOne();
        assertThat(eventRepository.count()).isOne();
        assertThat(healthRepository.count()).isOne();
    }

    @Test
    void shouldStoreConcurrentDistinctMessagesForSameDevice() throws Exception {
        int sequences = 8;
        List<Callable<IngestionResult>> tasks = new ArrayList<>();
        for (int i = 0; i < sequences; i++) {
            long sequence = i;
            tasks.add(() -> ingestionService.ingestTelemetry(
                    "concurrent-device", telemetry(SESSION_A, sequence, "31.7", 1750)));
            tasks.add(() -> ingestionService.ingestEvent(
                    "concurrent-device", event(SESSION_A, sequence, "STATUS")));
            tasks.add(() -> ingestionService.ingestHealth(
                    "concurrent-device", health(SESSION_A, sequence, 1)));
        }

        List<IngestionResult> results = runConcurrently(tasks);

        assertThat(results).hasSize(24).containsOnly(IngestionResult.STORED);
        assertThat(deviceRepository.count()).isOne();
        assertThat(telemetryRepository.count()).isEqualTo(sequences);
        assertThat(eventRepository.count()).isEqualTo(sequences);
        assertThat(healthRepository.count()).isEqualTo(sequences);
    }

    @Test
    void shouldStoreConcurrentTelemetryForDifferentDevices() throws Exception {
        int deviceCount = 12;
        List<Callable<IngestionResult>> tasks = new ArrayList<>();
        for (int i = 0; i < deviceCount; i++) {
            String deviceId = "parallel-device-" + i;
            long sequence = i;
            tasks.add(() -> ingestionService.ingestTelemetry(
                    deviceId, telemetry(SESSION_A, sequence, "31.7", 1750)));
        }

        List<IngestionResult> results = runConcurrently(tasks);

        assertThat(results).hasSize(deviceCount).containsOnly(IngestionResult.STORED);
        assertThat(deviceRepository.count()).isEqualTo(deviceCount);
        assertThat(telemetryRepository.count()).isEqualTo(deviceCount);
    }

    @Test
    void shouldDeduplicateConcurrentTelemetryAndRegisterDeviceAtomically() throws Exception {
        int attempts = 12;
        List<Callable<IngestionResult>> tasks = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> ingestionService.ingestTelemetry(
                    "concurrent-device", telemetry(SESSION_A, 99, "31.7", 1750)));
        }

        List<IngestionResult> results = runConcurrently(tasks);

        assertThat(results).filteredOn(IngestionResult.STORED::equals).hasSize(1);
        assertThat(results).filteredOn(IngestionResult.DUPLICATE::equals).hasSize(attempts - 1);
        assertThat(telemetryRepository.count()).isOne();
        assertThat(deviceRepository.count()).isOne();
    }

    @Test
    void shouldUpdateSavedAndDuplicateMetrics() {
        double savedBefore = counter("industrial_telemetry_records_saved_total");
        double duplicatesBefore = duplicateCounter("telemetry");

        ingestionService.ingestTelemetry("metrics-device", telemetry(SESSION_A, 3, "31.7", 1750));
        ingestionService.ingestTelemetry("metrics-device", telemetry(SESSION_A, 3, "31.7", 1750));

        assertThat(counter("industrial_telemetry_records_saved_total") - savedBefore).isEqualTo(1.0);
        assertThat(duplicateCounter("telemetry") - duplicatesBefore).isEqualTo(1.0);
    }

    private double counter(String name) {
        return meterRegistry.get(name).counter().count();
    }

    private double duplicateCounter(String messageType) {
        return meterRegistry.get("industrial_mqtt_messages_duplicate_total")
                .tag("message_type", messageType).counter().count();
    }

    private TelemetryMessage telemetry(String sessionId, long sequence, String temperature, int rpm) {
        return new TelemetryMessage(2, 123000L + sequence, sequence, sessionId,
                new BigDecimal(temperature), rpm);
    }

    private EventMessage event(String sessionId, long sequence, String type) {
        return new EventMessage(2, 123000L + sequence, sequence, sessionId, type);
    }

    private HealthMessage health(String sessionId, long sequence, int state) {
        return new HealthMessage(2, 123000L + sequence, sequence, sessionId, state,
                true, true, 0, 0L, 3600L, 0L, sequence, 0L, 0);
    }

    private List<IngestionResult> runConcurrently(List<Callable<IngestionResult>> tasks) throws Exception {
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(tasks.size())) {
            List<Future<IngestionResult>> futures = tasks.stream()
                    .map(task -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return task.call();
                    }))
                    .toList();
            ready.await();
            start.countDown();

            List<IngestionResult> results = new ArrayList<>();
            for (Future<IngestionResult> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }
}
