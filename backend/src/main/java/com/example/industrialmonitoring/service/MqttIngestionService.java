package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.EventMessage;
import com.example.industrialmonitoring.dto.HealthMessage;
import com.example.industrialmonitoring.dto.TelemetryMessage;
import com.example.industrialmonitoring.entity.EventRecordEntity;
import com.example.industrialmonitoring.entity.HealthRecordEntity;
import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import com.example.industrialmonitoring.repository.EventRecordRepository;
import com.example.industrialmonitoring.repository.HealthRecordRepository;
import com.example.industrialmonitoring.repository.TelemetryRecordRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MqttIngestionService {

    private final DeviceService deviceService;
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final EventRecordRepository eventRecordRepository;
    private final HealthRecordRepository healthRecordRepository;

    private final Counter telemetryRecordsSavedCounter;
    private final Counter eventRecordsSavedCounter;
    private final Counter healthRecordsSavedCounter;

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
    }

    @Transactional
    public void ingestTelemetry(String deviceId, TelemetryMessage message) {
        deviceService.ensureDeviceExists(deviceId);

        TelemetryRecordEntity entity = new TelemetryRecordEntity(
                deviceId,
                message.ts(),
                message.seq(),
                message.temperatureC(),
                message.rpm()
        );

        telemetryRecordRepository.save(entity);
        telemetryRecordsSavedCounter.increment();
    }

    @Transactional
    public void ingestEvent(String deviceId, EventMessage message) {
        deviceService.ensureDeviceExists(deviceId);

        EventRecordEntity entity = new EventRecordEntity(
                deviceId,
                message.ts(),
                message.seq(),
                message.eventType()
        );

        eventRecordRepository.save(entity);
        eventRecordsSavedCounter.increment();
    }

    @Transactional
    public void ingestHealth(String deviceId, HealthMessage message) {
        deviceService.ensureDeviceExists(deviceId);

        HealthRecordEntity entity = new HealthRecordEntity(
                deviceId,
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

        healthRecordRepository.save(entity);
        healthRecordsSavedCounter.increment();
    }
}