package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.dto.TelemetryRecordResponse;
import com.example.industrialmonitoring.service.TelemetryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;

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

    @GetMapping("/paged")
    public Page<TelemetryRecordResponse> getTelemetryRecordsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return telemetryService.findAllTelemetryRecords(
                PageRequest.of(page, size)
        );
    }

    @GetMapping("/latest")
    public TelemetryRecordResponse getLatestTelemetryRecord() {
        return telemetryService.findLatestTelemetryRecord();
    }

    @GetMapping("/device/{deviceId}")
    public List<TelemetryRecordResponse> getTelemetryRecordsByDeviceId(
            @PathVariable String deviceId
    ) {
        return telemetryService.findTelemetryRecordsByDeviceId(deviceId);
    }

    @GetMapping("/device/{deviceId}/paged")
    public Page<TelemetryRecordResponse> getTelemetryRecordsByDeviceIdPaged(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return telemetryService.findTelemetryRecordsByDeviceId(
                deviceId,
                PageRequest.of(page, size)
        );
    }
    @GetMapping("/device/{deviceId}/range")
public Page<TelemetryRecordResponse> getTelemetryRecordsByDeviceIdAndTimeRange(
        @PathVariable String deviceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
) {
    return telemetryService.findTelemetryRecordsByDeviceIdAndCreatedAtBetween(
            deviceId,
            from,
            to,
            PageRequest.of(page, size)
    );
}
}