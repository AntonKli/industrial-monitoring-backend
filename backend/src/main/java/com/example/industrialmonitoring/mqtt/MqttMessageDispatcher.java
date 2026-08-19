package com.example.industrialmonitoring.mqtt;

import com.example.industrialmonitoring.dto.EventMessage;
import com.example.industrialmonitoring.dto.HealthMessage;
import com.example.industrialmonitoring.dto.TelemetryMessage;
import com.example.industrialmonitoring.service.MqttIngestionService;
import org.springframework.stereotype.Component;

@Component
public class MqttMessageDispatcher {

    private final MqttTopicParser mqttTopicParser;
    private final MqttMessageParser mqttMessageParser;
    private final MqttIngestionService mqttIngestionService;

    public MqttMessageDispatcher(
            MqttTopicParser mqttTopicParser,
            MqttMessageParser mqttMessageParser,
            MqttIngestionService mqttIngestionService
    ) {
        this.mqttTopicParser = mqttTopicParser;
        this.mqttMessageParser = mqttMessageParser;
        this.mqttIngestionService = mqttIngestionService;
    }

    public void dispatch(String topic, String payload) {
        MqttTopicInfo topicInfo = mqttTopicParser.parse(topic);

        switch (topicInfo.messageType()) {
            case TELEMETRY -> handleTelemetry(topicInfo.deviceId(), payload);
            case EVENTS -> handleEvent(topicInfo.deviceId(), payload);
            case HEALTH -> handleHealth(topicInfo.deviceId(), payload);
        }
    }

    private void handleTelemetry(String deviceId, String payload) {
        TelemetryMessage message =
                mqttMessageParser.parse(payload, TelemetryMessage.class);

        mqttIngestionService.ingestTelemetry(deviceId, message);
    }

    private void handleEvent(String deviceId, String payload) {
        EventMessage message =
                mqttMessageParser.parse(payload, EventMessage.class);

        mqttIngestionService.ingestEvent(deviceId, message);
    }

    private void handleHealth(String deviceId, String payload) {
        HealthMessage message =
                mqttMessageParser.parse(payload, HealthMessage.class);

        mqttIngestionService.ingestHealth(deviceId, message);
    }
}
