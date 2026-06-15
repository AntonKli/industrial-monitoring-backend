package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.DeviceResponse;
import com.example.industrialmonitoring.entity.DeviceEntity;
import com.example.industrialmonitoring.exception.DeviceNotFoundException;
import com.example.industrialmonitoring.mapper.DeviceMapper;
import com.example.industrialmonitoring.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    public DeviceService(DeviceRepository deviceRepository, DeviceMapper deviceMapper) {
        this.deviceRepository = deviceRepository;
        this.deviceMapper = deviceMapper;
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> findAllDevices() {
        return deviceRepository.findAll()
                .stream()
                .map(deviceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponse findDeviceByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .map(deviceMapper::toResponse)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    @Transactional
    public void ensureDeviceExists(String deviceId) {
        if (!deviceRepository.existsByDeviceId(deviceId)) {
            deviceRepository.save(new DeviceEntity(deviceId));
        }
    }
}