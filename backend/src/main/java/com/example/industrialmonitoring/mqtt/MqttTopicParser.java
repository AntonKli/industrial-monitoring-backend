package com.example.industrialmonitoring.mqtt;

import com.example.industrialmonitoring.config.MqttProperties;
import org.springframework.stereotype.Component;

@Component
public class MqttTopicParser {

    private static final int EXPECTED_TOPIC_SEGMENTS = 3;
    private static final int MAX_DEVICE_ID_LENGTH = 100;

    private final MqttProperties mqttProperties;

    public MqttTopicParser(MqttProperties mqttProperties) {
        this.mqttProperties = mqttProperties;
    }

    public MqttTopicInfo parse(String topic) {
        if (topic == null || topic.isBlank()) {
            throw invalidTopic("MQTT topic must not be blank");
        }

        String[] parts = topic.split("/", -1);

        if (parts.length != EXPECTED_TOPIC_SEGMENTS) {
            throw invalidTopic("MQTT topic must contain exactly three segments");
        }

        if (!mqttProperties.topicRoot().equals(parts[0])) {
            throw invalidTopic("MQTT topic root does not match the configured root");
        }

        if (parts[1].isBlank()) {
            throw invalidTopic("MQTT device ID must not be blank");
        }

        if (parts[1].length() > MAX_DEVICE_ID_LENGTH) {
            throw invalidTopic("MQTT device ID must not exceed 100 characters");
        }

        return new MqttTopicInfo(
                parts[0],
                parts[1],
                MqttMessageType.fromTopicSegment(parts[2])
        );
    }

    private InvalidMqttMessageException invalidTopic(String message) {
        return new InvalidMqttMessageException(
                MqttMessageErrorType.INVALID_TOPIC,
                message
        );
    }
}
