package com.example.industrialmonitoring.mqtt;

public enum MqttMessageType {
    TELEMETRY("telemetry"),
    EVENTS("events"),
    HEALTH("health");

    private final String topicSegment;

    MqttMessageType(String topicSegment) {
        this.topicSegment = topicSegment;
    }

    public static MqttMessageType fromTopicSegment(String topicSegment) {
        for (MqttMessageType messageType : values()) {
            if (messageType.topicSegment.equals(topicSegment)) {
                return messageType;
            }
        }

        throw new InvalidMqttMessageException(
                MqttMessageErrorType.UNSUPPORTED_MESSAGE_TYPE,
                "Unsupported MQTT message type"
        );
    }
}
