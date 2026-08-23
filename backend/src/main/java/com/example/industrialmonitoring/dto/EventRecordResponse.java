package com.example.industrialmonitoring.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventRecordResponse(
        Long id,
        String deviceId,
        Long gatewayTimestamp,
        Long sequenceNumber,
        UUID sessionId,
        Long sessionGeneration,
        String eventType,
        OffsetDateTime createdAt
) {
}
