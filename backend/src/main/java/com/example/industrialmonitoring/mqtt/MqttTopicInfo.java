package com.example.industrialmonitoring.mqtt;

public record MqttTopicInfo(
        String root,
        String deviceId,
        String messageType
) {
}