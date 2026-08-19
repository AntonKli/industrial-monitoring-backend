package com.example.industrialmonitoring.mqtt;

import com.example.industrialmonitoring.config.MqttProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MqttTopicParserTest {

    private MqttTopicParser parser;

    @BeforeEach
    void setUp() {
        MqttProperties properties = new MqttProperties(
                "tcp://localhost:1883",
                "test-client",
                "rtz",
                "edge01",
                "edge",
                "password"
        );

        parser = new MqttTopicParser(properties);
    }

    @Test
    void shouldParseTelemetryTopic() {
        MqttTopicInfo topicInfo = parser.parse("rtz/edge01/telemetry");

        assertThat(topicInfo.root()).isEqualTo("rtz");
        assertThat(topicInfo.deviceId()).isEqualTo("edge01");
        assertThat(topicInfo.messageType()).isEqualTo(MqttMessageType.TELEMETRY);
    }

    @Test
    void shouldParseEventTopic() {
        assertThat(parser.parse("rtz/edge01/events").messageType())
                .isEqualTo(MqttMessageType.EVENTS);
    }

    @Test
    void shouldParseHealthTopic() {
        assertThat(parser.parse("rtz/edge01/health").messageType())
                .isEqualTo(MqttMessageType.HEALTH);
    }

    @Test
    void shouldRejectWrongTopicRoot() {
        assertErrorType(
                "other/edge01/telemetry",
                MqttMessageErrorType.INVALID_TOPIC
        );
    }

    @Test
    void shouldRejectBlankDeviceId() {
        assertErrorType(
                "rtz//telemetry",
                MqttMessageErrorType.INVALID_TOPIC
        );
    }

    @Test
    void shouldRejectDeviceIdLongerThanDatabaseColumn() {
        assertErrorType(
                "rtz/" + "a".repeat(101) + "/telemetry",
                MqttMessageErrorType.INVALID_TOPIC
        );
    }

    @Test
    void shouldRejectMissingTopicSegment() {
        assertErrorType(
                "rtz/edge01",
                MqttMessageErrorType.INVALID_TOPIC
        );
    }

    @Test
    void shouldRejectAdditionalTopicSegment() {
        assertErrorType(
                "rtz/edge01/telemetry/additional",
                MqttMessageErrorType.INVALID_TOPIC
        );
    }

    @Test
    void shouldRejectUnsupportedMessageType() {
        assertErrorType(
                "rtz/edge01/unknown",
                MqttMessageErrorType.UNSUPPORTED_MESSAGE_TYPE
        );
    }

    private void assertErrorType(
            String topic,
            MqttMessageErrorType expectedErrorType
    ) {
        assertThatThrownBy(() -> parser.parse(topic))
                .isInstanceOfSatisfying(
                        InvalidMqttMessageException.class,
                        exception -> assertThat(exception.getErrorType())
                                .isEqualTo(expectedErrorType)
                );
    }
}
