package com.example.industrialmonitoring.mqtt;

import com.example.industrialmonitoring.config.MqttProperties;
import org.springframework.stereotype.Component;

@Component
public class MqttTopicResolver {

    private final MqttProperties mqttProperties;

    public MqttTopicResolver(MqttProperties mqttProperties) {
        this.mqttProperties = mqttProperties;
    }

    public String telemetryTopic() {
        return mqttProperties.topicRoot() + "/+/telemetry";
    }

    public String eventsTopic() {
        return mqttProperties.topicRoot() + "/+/events";
    }

    public String healthTopic() {
        return mqttProperties.topicRoot() + "/+/health";
    }
}