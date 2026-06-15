package com.example.industrialmonitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mqtt")
public record MqttProperties(
        String brokerUrl,
        String clientId,
        String topicRoot,
        String deviceId,
        String username,
        String password
) {
}