package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.dto.HealthRecordResponse;
import com.example.industrialmonitoring.service.HealthService;
import org.springframework.web.bind.annotation.*;

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
    public HealthRecordResponse getLatestHealthRecord() {
        return healthService.findLatestHealthRecord();
    }

    @GetMapping("/device/{deviceId}")
    public List<HealthRecordResponse> getHealthRecordsByDeviceId(@PathVariable String deviceId) {
        return healthService.findHealthRecordsByDeviceId(deviceId);
    }
}