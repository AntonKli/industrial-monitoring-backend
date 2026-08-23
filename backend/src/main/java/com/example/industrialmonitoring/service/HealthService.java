package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.HealthRecordResponse;
import com.example.industrialmonitoring.exception.HealthRecordNotFoundException;
import com.example.industrialmonitoring.mapper.HealthRecordMapper;
import com.example.industrialmonitoring.repository.HealthRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HealthService {

    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordMapper healthRecordMapper;

    public HealthService(
            HealthRecordRepository healthRecordRepository,
            HealthRecordMapper healthRecordMapper
    ) {
        this.healthRecordRepository = healthRecordRepository;
        this.healthRecordMapper = healthRecordMapper;
    }

    @Transactional(readOnly = true)
    public List<HealthRecordResponse> findAllHealthRecords() {
        return healthRecordRepository.findAllByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(healthRecordMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HealthRecordResponse findLatestReceivedHealthRecord() {
        return healthRecordRepository.findFirstByOrderByCreatedAtDescIdDesc()
                .map(healthRecordMapper::toResponse)
                .orElseThrow(HealthRecordNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public HealthRecordResponse findLatestReceivedHealthRecordByDeviceId(String deviceId) {
        return healthRecordRepository.findFirstByDeviceIdOrderByCreatedAtDescIdDesc(deviceId)
                .map(healthRecordMapper::toResponse)
                .orElseThrow(HealthRecordNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public HealthRecordResponse findLatestObservedHealthRecordByDeviceId(String deviceId) {
        return healthRecordRepository.findLatestObservedByDeviceId(deviceId)
                .map(healthRecordMapper::toResponse)
                .orElseThrow(HealthRecordNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<HealthRecordResponse> findHealthRecordsByDeviceId(String deviceId) {
        return healthRecordRepository.findByDeviceIdOrderByCreatedAtDescIdDesc(deviceId)
                .stream()
                .map(healthRecordMapper::toResponse)
                .toList();
    }
}
