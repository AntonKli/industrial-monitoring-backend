package com.example.industrialmonitoring.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TelemetryRecordResponse(
        Long id,
        String deviceId,
        Long gatewayTimestamp,
        Long sequenceNumber,
        BigDecimal temperatureC,
        Integer rpm,
        OffsetDateTime createdAt
) {
}