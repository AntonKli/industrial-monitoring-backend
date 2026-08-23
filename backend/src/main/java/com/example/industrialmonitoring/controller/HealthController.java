package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.dto.HealthRecordResponse;
import com.example.industrialmonitoring.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public List<HealthRecordResponse> getAllHealthRecords() {
        return healthService.findAllHealthRecords();
    }

    @GetMapping("/latest")
    @Deprecated(since = "1.4b")
    public HealthRecordResponse getLatestHealthRecord() {
        return healthService.findLatestReceivedHealthRecord();
    }

    @GetMapping("/latest-received")
    public HealthRecordResponse getLatestReceivedHealthRecord() {
        return healthService.findLatestReceivedHealthRecord();
    }

    @GetMapping("/device/{deviceId}/latest-received")
    public HealthRecordResponse getLatestReceivedHealthRecordByDeviceId(
            @PathVariable String deviceId
    ) {
        return healthService.findLatestReceivedHealthRecordByDeviceId(deviceId);
    }

    @GetMapping("/device/{deviceId}/latest-observed")
    public HealthRecordResponse getLatestObservedHealthRecordByDeviceId(
            @PathVariable String deviceId
    ) {
        return healthService.findLatestObservedHealthRecordByDeviceId(deviceId);
    }

    @GetMapping("/device/{deviceId}")
    public List<HealthRecordResponse> getHealthRecordsByDeviceId(@PathVariable String deviceId) {
        return healthService.findHealthRecordsByDeviceId(deviceId);
    }
}
