package com.example.industrialmonitoring.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MqttMessageParser {

    private final ObjectMapper objectMapper;

    public MqttMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T parse(String payload, Class<T> targetType) {
        try {
            return objectMapper.readValue(payload, targetType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to parse MQTT payload",
                    exception
            );
        }
    }
}