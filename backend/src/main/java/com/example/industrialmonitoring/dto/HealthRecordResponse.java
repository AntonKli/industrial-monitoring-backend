package com.example.industrialmonitoring.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HealthRecordResponse(
        Long id,
        String deviceId,
        Long gatewayTimestamp,
        Long sequenceNumber,
        UUID sessionId,
        Long sessionGeneration,
        Integer state,
        Boolean mqttConnected,
        Boolean pubLastOk,
        Integer bufferFill,
        Long bufferDrops,
        Long diagUptimeS,
        Long diagReconnects,
        Long diagPubOk,
        Long diagPubFail,
        Integer diagLastError,
        OffsetDateTime createdAt
) {
}
