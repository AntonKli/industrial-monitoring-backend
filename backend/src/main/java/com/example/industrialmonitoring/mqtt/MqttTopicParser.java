package com.example.industrialmonitoring.mqtt;

import org.springframework.stereotype.Component;

@Component
public class MqttTopicParser {

    public MqttTopicInfo parse(String topic) {
        String[] parts = topic.split("/");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid MQTT topic: " + topic);
        }

        return new MqttTopicInfo(
                parts[0],
                parts[1],
                parts[2]
        );
    }
}