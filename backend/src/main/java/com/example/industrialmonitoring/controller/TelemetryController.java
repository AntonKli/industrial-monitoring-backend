package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.dto.TelemetryRecordResponse;
import com.example.industrialmonitoring.service.TelemetryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping
    public List<TelemetryRecordResponse> getAllTelemetryRecords() {
        return telemetryService.findAllTelemetryRecords();
    }

    @GetMapping("/latest")
    public TelemetryRecordResponse getLatestTelemetryRecord() {
        return telemetryService.findLatestTelemetryRecord();
    }

    @GetMapping("/device/{deviceId}")
    public List<TelemetryRecordResponse> getTelemetryRecordsByDeviceId(@PathVariable String deviceId) {
        return telemetryService.findTelemetryRecordsByDeviceId(deviceId);
    }
}