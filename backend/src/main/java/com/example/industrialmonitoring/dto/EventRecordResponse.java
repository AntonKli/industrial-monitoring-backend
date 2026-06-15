package com.example.industrialmonitoring.dto;

import java.time.OffsetDateTime;

public record EventRecordResponse(
        Long id,
        String deviceId,
        Long gatewayTimestamp,
        Long sequenceNumber,
        String eventType,
        OffsetDateTime createdAt
) {
}