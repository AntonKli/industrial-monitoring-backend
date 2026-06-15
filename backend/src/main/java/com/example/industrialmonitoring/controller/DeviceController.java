package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.dto.DeviceResponse;
import com.example.industrialmonitoring.service.DeviceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceResponse> getAllDevices() {
        return deviceService.findAllDevices();
    }

    @GetMapping("/{deviceId}")
    public DeviceResponse getDeviceByDeviceId(
        @PathVariable String deviceId
) {
    return deviceService.findDeviceByDeviceId(deviceId);
}
}