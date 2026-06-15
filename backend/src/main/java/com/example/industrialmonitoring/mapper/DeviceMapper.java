package com.example.industrialmonitoring.mapper;

import com.example.industrialmonitoring.dto.DeviceResponse;
import com.example.industrialmonitoring.entity.DeviceEntity;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {

    public DeviceResponse toResponse(DeviceEntity entity) {
        return new DeviceResponse(
                entity.getId(),
                entity.getDeviceId(),
                entity.getCreatedAt()
        );
    }
}