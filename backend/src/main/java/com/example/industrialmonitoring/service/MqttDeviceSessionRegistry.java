package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.mqtt.InvalidMqttMessageException;
import com.example.industrialmonitoring.mqtt.MqttMessageErrorType;
import com.example.industrialmonitoring.repository.MqttDeviceSessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class MqttDeviceSessionRegistry {

    private final MqttDeviceSessionRepository repository;

    public MqttDeviceSessionRegistry(MqttDeviceSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void register(
            String deviceId,
            Long sessionGeneration,
            UUID sessionId
    ) {
        int inserted = repository.insertIfAbsent(
                deviceId,
                sessionGeneration,
                sessionId
        );

        if (inserted == 1 || repository.existsByDeviceIdAndSessionGenerationAndSessionId(
                deviceId,
                sessionGeneration,
                sessionId
        )) {
            return;
        }

        throw new InvalidMqttMessageException(
                MqttMessageErrorType.SESSION_MAPPING_CONFLICT,
                "Protocol v3 session mapping conflicts with the registered device session"
        );
    }
}
