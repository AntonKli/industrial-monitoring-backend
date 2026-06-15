package com.example.industrialmonitoring.mapper;

import com.example.industrialmonitoring.dto.EventRecordResponse;
import com.example.industrialmonitoring.entity.EventRecordEntity;
import org.springframework.stereotype.Component;

@Component
public class EventRecordMapper {

    public EventRecordResponse toResponse(EventRecordEntity entity) {
        return new EventRecordResponse(
                entity.getId(),
                entity.getDeviceId(),
                entity.getGatewayTimestamp(),
                entity.getSequenceNumber(),
                entity.getEventType(),
                entity.getCreatedAt()
        );
    }
}