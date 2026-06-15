package com.example.industrialmonitoring.mapper;

import com.example.industrialmonitoring.dto.TelemetryRecordResponse;
import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import org.springframework.stereotype.Component;

@Component
public class TelemetryRecordMapper {

    public TelemetryRecordResponse toResponse(TelemetryRecordEntity entity) {
        return new TelemetryRecordResponse(
                entity.getId(),
                entity.getDeviceId(),
                entity.getGatewayTimestamp(),
                entity.getSequenceNumber(),
                entity.getTemperatureC(),
                entity.getRpm(),
                entity.getCreatedAt()
        );
    }
}