package com.example.industrialmonitoring.mapper;

import com.example.industrialmonitoring.dto.HealthRecordResponse;
import com.example.industrialmonitoring.entity.HealthRecordEntity;
import org.springframework.stereotype.Component;

@Component
public class HealthRecordMapper {

    public HealthRecordResponse toResponse(HealthRecordEntity entity) {
        return new HealthRecordResponse(
                entity.getId(),
                entity.getDeviceId(),
                entity.getGatewayTimestamp(),
                entity.getSequenceNumber(),
                entity.getState(),
                entity.getMqttConnected(),
                entity.getPubLastOk(),
                entity.getBufferFill(),
                entity.getBufferDrops(),
                entity.getDiagUptimeS(),
                entity.getDiagReconnects(),
                entity.getDiagPubOk(),
                entity.getDiagPubFail(),
                entity.getDiagLastError(),
                entity.getCreatedAt()
        );
    }
}