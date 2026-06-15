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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MqttIngestionService {

    private final DeviceService deviceService;
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final EventRecordRepository eventRecordRepository;
    private final HealthRecordRepository healthRecordRepository;

    public MqttIngestionService(
            DeviceService deviceService,
            TelemetryRecordRepository telemetryRecordRepository,
            EventRecordRepository eventRecordRepository,
            HealthRecordRepository healthRecordRepository
    ) {
        this.deviceService = deviceService;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.eventRecordRepository = eventRecordRepository;
        this.healthRecordRepository = healthRecordRepository;
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
    }
}