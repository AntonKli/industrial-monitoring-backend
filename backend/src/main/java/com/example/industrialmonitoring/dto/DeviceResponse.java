package com.example.industrialmonitoring.dto;

import java.time.OffsetDateTime;

public record DeviceResponse(
        Long id,
        String deviceId,
        OffsetDateTime createdAt
) {
}