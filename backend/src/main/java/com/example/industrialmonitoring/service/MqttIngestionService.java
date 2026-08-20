package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.EventMessage;
import com.example.industrialmonitoring.dto.HealthMessage;
import com.example.industrialmonitoring.dto.TelemetryMessage;
import com.example.industrialmonitoring.repository.EventRecordRepository;
import com.example.industrialmonitoring.repository.HealthRecordRepository;
import com.example.industrialmonitoring.repository.TelemetryRecordRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MqttIngestionService {

    private final DeviceService deviceService;
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final EventRecordRepository eventRecordRepository;
    private final HealthRecordRepository healthRecordRepository;

    private final Counter telemetryRecordsSavedCounter;
    private final Counter eventRecordsSavedCounter;
    private final Counter healthRecordsSavedCounter;
    private final Counter telemetryDuplicatesCounter;
    private final Counter eventDuplicatesCounter;
    private final Counter healthDuplicatesCounter;

    public MqttIngestionService(
            DeviceService deviceService,
            TelemetryRecordRepository telemetryRecordRepository,
            EventRecordRepository eventRecordRepository,
            HealthRecordRepository healthRecordRepository,
            MeterRegistry meterRegistry
    ) {
        this.deviceService = deviceService;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.eventRecordRepository = eventRecordRepository;
        this.healthRecordRepository = healthRecordRepository;

        this.telemetryRecordsSavedCounter = Counter.builder("industrial_telemetry_records_saved_total")
                .description("Total number of persisted telemetry records")
                .register(meterRegistry);

        this.eventRecordsSavedCounter = Counter.builder("industrial_event_records_saved_total")
                .description("Total number of persisted event records")
                .register(meterRegistry);

        this.healthRecordsSavedCounter = Counter.builder("industrial_health_records_saved_total")
                .description("Total number of persisted health records")
                .register(meterRegistry);

        this.telemetryDuplicatesCounter = duplicateCounter(meterRegistry, "telemetry");
        this.eventDuplicatesCounter = duplicateCounter(meterRegistry, "event");
        this.healthDuplicatesCounter = duplicateCounter(meterRegistry, "health");
    }

    @Transactional
    public IngestionResult ingestTelemetry(String deviceId, TelemetryMessage message) {
        deviceService.ensureDeviceExists(deviceId);

        int inserted = telemetryRecordRepository.insertIfAbsent(
                deviceId,
                UUID.fromString(message.sessionId()),
                message.ts(),
                message.seq(),
                message.temperatureC(),
                message.rpm()
        );

        return result(inserted, telemetryRecordsSavedCounter, telemetryDuplicatesCounter);
    }

    @Transactional
    public IngestionResult ingestEvent(String deviceId, EventMessage message) {
        deviceService.ensureDeviceExists(deviceId);

        int inserted = eventRecordRepository.insertIfAbsent(
                deviceId,
                UUID.fromString(message.sessionId()),
                message.ts(),
                message.seq(),
                message.eventType()
        );

        return result(inserted, eventRecordsSavedCounter, eventDuplicatesCounter);
    }

    @Transactional
    public IngestionResult ingestHealth(String deviceId, HealthMessage message) {
        deviceService.ensureDeviceExists(deviceId);

        int inserted = healthRecordRepository.insertIfAbsent(
                deviceId,
                UUID.fromString(message.sessionId()),
                message.ts(),
                message.seq(),
                message.state(),
                message.mqttConnected(),
                message.pubLastOk(),
                message.bufferFill(),
                message.bufferDrops(),
                message.diagUptimeS(),
                message.diagReconnects(),
                message.diagPubOk(),
                message.diagPubFail(),
                message.diagLastError()
        );

        return result(inserted, healthRecordsSavedCounter, healthDuplicatesCounter);
    }

    private Counter duplicateCounter(MeterRegistry meterRegistry, String messageType) {
        return Counter.builder("industrial_mqtt_messages_duplicate_total")
                .description("Total number of duplicate MQTT messages")
                .tag("message_type", messageType)
                .register(meterRegistry);
    }

    private IngestionResult result(
            int inserted,
            Counter savedCounter,
            Counter duplicateCounter
    ) {
        if (inserted == 1) {
            savedCounter.increment();
            return IngestionResult.STORED;
        }

        duplicateCounter.increment();
        return IngestionResult.DUPLICATE;
    }
}
