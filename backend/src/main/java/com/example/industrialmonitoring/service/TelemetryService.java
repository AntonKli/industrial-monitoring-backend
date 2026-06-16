package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.TelemetryRecordResponse;
import com.example.industrialmonitoring.exception.TelemetryRecordNotFoundException;
import com.example.industrialmonitoring.mapper.TelemetryRecordMapper;
import com.example.industrialmonitoring.repository.TelemetryRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TelemetryService {

    private final TelemetryRecordRepository telemetryRecordRepository;
    private final TelemetryRecordMapper telemetryRecordMapper;

    public TelemetryService(
            TelemetryRecordRepository telemetryRecordRepository,
            TelemetryRecordMapper telemetryRecordMapper
    ) {
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.telemetryRecordMapper = telemetryRecordMapper;
    }

    @Transactional(readOnly = true)
    public List<TelemetryRecordResponse> findAllTelemetryRecords() {
        return telemetryRecordRepository.findAll()
                .stream()
                .map(telemetryRecordMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TelemetryRecordResponse> findAllTelemetryRecords(Pageable pageable) {
        return telemetryRecordRepository.findAll(pageable)
                .map(telemetryRecordMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TelemetryRecordResponse findLatestTelemetryRecord() {
        return telemetryRecordRepository.findFirstByOrderByCreatedAtDesc()
                .map(telemetryRecordMapper::toResponse)
                .orElseThrow(TelemetryRecordNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<TelemetryRecordResponse> findTelemetryRecordsByDeviceId(String deviceId) {
        return telemetryRecordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId)
                .stream()
                .map(telemetryRecordMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TelemetryRecordResponse> findTelemetryRecordsByDeviceId(String deviceId, Pageable pageable) {
        return telemetryRecordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId, pageable)
                .map(telemetryRecordMapper::toResponse);
    }
}