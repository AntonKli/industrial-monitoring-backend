package com.example.industrialmonitoring.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TelemetryRecordResponse(
        Long id,
        String deviceId,
        Long gatewayTimestamp,
        Long sequenceNumber,
        UUID sessionId,
        Long sessionGeneration,
        BigDecimal temperatureC,
        Integer rpm,
        OffsetDateTime createdAt
) {
}
