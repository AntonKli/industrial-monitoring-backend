package com.example.industrialmonitoring.mqtt;

import com.example.industrialmonitoring.dto.EventMessage;
import com.example.industrialmonitoring.dto.HealthMessage;
import com.example.industrialmonitoring.dto.TelemetryMessage;
import com.example.industrialmonitoring.dto.VersionedMqttMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MqttMessageParserTest {

    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private MqttMessageParser parser;

    @BeforeEach
    void setUp() {
        parser = new MqttMessageParser(
                new ObjectMapper(),
                Validation.buildDefaultValidatorFactory().getValidator()
        );
    }

    @Test
    void shouldParseValidProtocolV2TelemetryWithoutSessionGeneration() {
        TelemetryMessage message = parser.parse(telemetryPayload("3"), TelemetryMessage.class);
        assertThat(message.v()).isEqualTo(2);
        assertThat(message.seq()).isEqualTo(3L);
        assertThat(message.sessionId()).isEqualTo(SESSION_ID);
        assertThat(message.sessionGeneration()).isNull();
        assertThat(message.temperatureC()).isEqualByComparingTo("30.2");
    }

    @Test
    void shouldParseValidProtocolV3Telemetry() {
        TelemetryMessage message = parser.parse(v3TelemetryPayload("7"), TelemetryMessage.class);

        assertThat(message.v()).isEqualTo(3);
        assertThat(message.sessionGeneration()).isEqualTo(7L);
    }

    @Test
    void shouldParseValidProtocolV3Event() {
        EventMessage message = parser.parse(
                "{\"v\":3,\"ts\":123001,\"seq\":4,\"session_id\":\"" + SESSION_ID
                        + "\",\"session_generation\":7,\"type\":\"ALARM_RAISED\"}",
                EventMessage.class
        );

        assertThat(message.sessionGeneration()).isEqualTo(7L);
    }

    @Test
    void shouldParseValidProtocolV3Health() {
        HealthMessage message = parser.parse(
                "{\"v\":3,\"ts\":123002,\"seq\":5,\"session_id\":\"" + SESSION_ID
                        + "\",\"session_generation\":7}",
                HealthMessage.class
        );

        assertThat(message.sessionGeneration()).isEqualTo(7L);
    }

    @Test
    void shouldRejectProtocolV3WithoutSessionGeneration() {
        assertError(telemetryPayload("3").replace("\"v\":2", "\"v\":3"),
                MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldRejectZeroProtocolV3SessionGeneration() {
        assertError(v3TelemetryPayload("0"), MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldRejectNegativeProtocolV3SessionGeneration() {
        assertError(v3TelemetryPayload("-1"), MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldAcceptLongMaxProtocolV3SessionGeneration() {
        TelemetryMessage message = parser.parse(
                v3TelemetryPayload(Long.toString(Long.MAX_VALUE)),
                TelemetryMessage.class
        );

        assertThat(message.sessionGeneration()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void shouldRejectProtocolV3SessionGenerationAboveLongMax() {
        assertError(v3TelemetryPayload("9223372036854775808"),
                MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectFloatingProtocolV3SessionGeneration() {
        assertError(v3TelemetryPayload("7.5"), MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectStringProtocolV3SessionGeneration() {
        assertError(v3TelemetryPayload("\"7\""), MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldParseValidEventMessage() {
        EventMessage message = parser.parse(
                "{\"v\":2,\"ts\":123001,\"seq\":4,\"session_id\":\"" + SESSION_ID
                        + "\",\"type\":\"ALARM_RAISED\"}", EventMessage.class);
        assertThat(message.sessionId()).isEqualTo(SESSION_ID);
        assertThat(message.eventType()).isEqualTo("ALARM_RAISED");
    }

    @Test
    void shouldParseValidHealthMessage() {
        HealthMessage message = parser.parse(
                "{\"v\":2,\"ts\":123002,\"seq\":5,\"session_id\":\"" + SESSION_ID + "\"}",
                HealthMessage.class);
        assertThat(message.sessionId()).isEqualTo(SESSION_ID);
        assertThat(message.state()).isNull();
    }

    @Test
    void shouldRejectMissingSessionId() {
        assertError("{\"v\":2,\"ts\":123000,\"seq\":3}",
                MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldRejectNullSessionId() {
        assertInvalidSessionId("null");
    }

    @Test
    void shouldRejectEmptySessionId() {
        assertInvalidSessionId("\"\"");
    }

    @Test
    void shouldRejectNumericSessionId() {
        assertError("{\"v\":2,\"ts\":123000,\"seq\":3,\"session_id\":42}",
                MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectWrongLengthSessionId() {
        assertInvalidSessionId("\"550e8400-e29b-41d4-a716-44665544000\"");
    }

    @Test
    void shouldRejectNonHexSessionId() {
        assertInvalidSessionId("\"550e8400-e29b-41d4-a716-44665544000g\"");
    }

    @Test
    void shouldRejectWrongHyphenPosition() {
        assertInvalidSessionId("\"550e8400e-29b-41d4-a716-446655440000\"");
    }

    @Test
    void shouldRejectUppercaseSessionId() {
        assertInvalidSessionId("\"550E8400-E29B-41D4-A716-446655440000\"");
    }

    @Test
    void shouldRejectProtocolVersionOne() {
        assertError(telemetryPayload("3").replace("\"v\":2", "\"v\":1"),
                MqttMessageErrorType.UNSUPPORTED_PROTOCOL_VERSION);
    }

    @Test
    void shouldRejectUnknownProtocolVersion() {
        assertError(telemetryPayload("3").replace("\"v\":2", "\"v\":4"),
                MqttMessageErrorType.UNSUPPORTED_PROTOCOL_VERSION);
    }

    @Test
    void shouldRejectMissingProtocolVersion() {
        assertError(telemetryPayload("3").replace("\"v\":2,", ""),
                MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldRejectNegativeSequenceNumber() {
        assertError(telemetryPayload("-1"), MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldAcceptZeroSequenceNumber() {
        assertThat(parser.parse(telemetryPayload("0"), TelemetryMessage.class).seq()).isZero();
    }

    @Test
    void shouldAcceptLongMaxSequenceNumber() {
        assertThat(parser.parse(telemetryPayload(Long.toString(Long.MAX_VALUE)), TelemetryMessage.class).seq())
                .isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void shouldRejectSequenceNumberAboveLongMax() {
        assertError(telemetryPayload("9223372036854775808"), MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectStringSequenceNumber() {
        assertError(telemetryPayload("\"42\""), MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectFloatingSequenceNumber() {
        assertError(telemetryPayload("42.5"), MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectMissingSequenceNumber() {
        assertError(telemetryPayload("3").replace("\"seq\":3,", ""),
                MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldRejectMalformedJson() {
        assertError("{\"v\":2,", MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectBlankPayload() {
        assertError("  ", MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectTemperatureOutsideDatabasePrecision() {
        assertError(telemetryPayload("3").replace("30.2", "12345.67"),
                MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldRejectBlankEventType() {
        assertErrorType(
                "{\"v\":2,\"ts\":123001,\"seq\":4,\"session_id\":\"" + SESSION_ID
                        + "\",\"type\":\" \"}", EventMessage.class,
                MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldRejectStringProtocolVersion() {
        assertError(telemetryPayload("3").replace("\"v\":2", "\"v\":\"2\""),
                MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectFloatingProtocolVersion() {
        assertError(telemetryPayload("3").replace("\"v\":2", "\"v\":2.0"),
                MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectStringTimestamp() {
        assertError(telemetryPayload("3").replace("\"ts\":123000", "\"ts\":\"123000\""),
                MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectMissingTimestamp() {
        assertError(telemetryPayload("3").replace("\"ts\":123000,", ""),
                MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldRejectStringTemperature() {
        assertError(telemetryPayload("3").replace("30.2", "\"30.2\""),
                MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectStringBoolean() {
        assertErrorType(
                healthPayload("\"true\""), HealthMessage.class,
                MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectNumericBoolean() {
        assertErrorType(healthPayload("1"), HealthMessage.class,
                MqttMessageErrorType.MALFORMED_JSON);
    }

    @Test
    void shouldRejectEventTypeLongerThanDatabaseColumn() {
        assertErrorType(
                "{\"v\":2,\"ts\":123001,\"seq\":4,\"session_id\":\"" + SESSION_ID
                        + "\",\"type\":\"" + "x".repeat(101) + "\"}",
                EventMessage.class,
                MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    @Test
    void shouldNotIncludePayloadInValidationError() {
        String sensitiveValue = "sensitive-event-value" + "x".repeat(100);

        assertThatThrownBy(() -> parser.parse(
                "{\"v\":2,\"ts\":123001,\"seq\":4,\"session_id\":\"" + SESSION_ID
                        + "\",\"type\":\"" + sensitiveValue + "\"}", EventMessage.class))
                .isInstanceOf(InvalidMqttMessageException.class)
                .hasMessageNotContaining(sensitiveValue);
    }

    private String telemetryPayload(String sequenceNumber) {
        return "{\"v\":2,\"ts\":123000,\"seq\":" + sequenceNumber
                + ",\"session_id\":\"" + SESSION_ID + "\",\"temp_c\":30.2,\"rpm\":1600}";
    }

    private String v3TelemetryPayload(String sessionGeneration) {
        return "{\"v\":3,\"ts\":123000,\"seq\":3,\"session_id\":\"" + SESSION_ID
                + "\",\"session_generation\":" + sessionGeneration
                + ",\"temp_c\":30.2,\"rpm\":1600}";
    }

    private String healthPayload(String mqttConnected) {
        return "{\"v\":2,\"ts\":123002,\"seq\":5,\"session_id\":\"" + SESSION_ID
                + "\",\"mqtt_connected\":" + mqttConnected + "}";
    }

    private void assertInvalidSessionId(String jsonValue) {
        assertError("{\"v\":2,\"ts\":123000,\"seq\":3,\"session_id\":" + jsonValue + "}",
                MqttMessageErrorType.CONSTRAINT_VIOLATION);
    }

    private void assertError(String payload, MqttMessageErrorType expectedErrorType) {
        assertErrorType(payload, TelemetryMessage.class, expectedErrorType);
    }

    private <T extends VersionedMqttMessage> void assertErrorType(
            String payload, Class<T> targetType, MqttMessageErrorType expectedErrorType) {
        assertThatThrownBy(() -> parser.parse(payload, targetType))
                .isInstanceOfSatisfying(
                        InvalidMqttMessageException.class,
                        exception -> assertThat(exception.getErrorType()).isEqualTo(expectedErrorType)
                );
    }
}
