package com.example.industrialmonitoring.mqtt;

import com.example.industrialmonitoring.dto.EventMessage;
import com.example.industrialmonitoring.dto.HealthMessage;
import com.example.industrialmonitoring.dto.TelemetryMessage;
import com.example.industrialmonitoring.dto.VersionedMqttMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MqttMessageParserTest {

    private MqttMessageParser parser;

    @BeforeEach
    void setUp() {
        Validator validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();

        parser = new MqttMessageParser(
                new ObjectMapper(),
                validator
        );
    }

    @Test
    void shouldParseValidTelemetryMessage() {
        TelemetryMessage message = parser.parse(
                """
                        {
                          "v": 1,
                          "ts": 123000,
                          "seq": 3,
                          "temp_c": 30.2,
                          "rpm": 1600
                        }
                        """,
                TelemetryMessage.class
        );

        assertThat(message.v()).isEqualTo(1);
        assertThat(message.ts()).isEqualTo(123000L);
        assertThat(message.seq()).isEqualTo(3L);
        assertThat(message.temperatureC()).isEqualByComparingTo("30.2");
        assertThat(message.rpm()).isEqualTo(1600);
    }

    @Test
    void shouldParseValidEventMessage() {
        EventMessage message = parser.parse(
                """
                        {
                          "v": 1,
                          "ts": 123001,
                          "seq": 4,
                          "type": "ALARM_RAISED"
                        }
                        """,
                EventMessage.class
        );

        assertThat(message.eventType()).isEqualTo("ALARM_RAISED");
    }

    @Test
    void shouldParseValidHealthMessageWithOptionalFieldsMissing() {
        HealthMessage message = parser.parse(
                """
                        {
                          "v": 1,
                          "ts": 123002,
                          "seq": 5
                        }
                        """,
                HealthMessage.class
        );

        assertThat(message.v()).isEqualTo(1);
        assertThat(message.state()).isNull();
        assertThat(message.mqttConnected()).isNull();
    }

    @Test
    void shouldRejectHealthMessageWithoutSequenceNumber() {
        assertErrorType(
                "{\"v\":1,\"ts\":123002}",
                HealthMessage.class,
                MqttMessageErrorType.CONSTRAINT_VIOLATION
        );
    }

    @Test
    void shouldRejectMalformedJson() {
        assertErrorType(
                "{\"v\":1,",
                TelemetryMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectBlankPayload() {
        assertErrorType(
                "  ",
                TelemetryMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectMissingProtocolVersion() {
        assertErrorType(
                "{\"ts\":123000,\"seq\":3}",
                TelemetryMessage.class,
                MqttMessageErrorType.CONSTRAINT_VIOLATION
        );
    }

    @Test
    void shouldRejectUnsupportedProtocolVersion() {
        assertErrorType(
                "{\"v\":2,\"ts\":123000,\"seq\":3}",
                TelemetryMessage.class,
                MqttMessageErrorType.UNSUPPORTED_PROTOCOL_VERSION
        );
    }

    @Test
    void shouldRejectStringProtocolVersion() {
        assertErrorType(
                "{\"v\":\"1\",\"ts\":123000,\"seq\":3}",
                TelemetryMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectStringTimestamp() {
        assertErrorType(
                "{\"v\":1,\"ts\":\"123\",\"seq\":3}",
                TelemetryMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectStringSequenceNumber() {
        assertErrorType(
                "{\"v\":1,\"ts\":123000,\"seq\":\"42\"}",
                TelemetryMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectFloatingProtocolVersion() {
        assertErrorType(
                "{\"v\":1.0,\"ts\":123000,\"seq\":3}",
                TelemetryMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectFloatingSequenceNumber() {
        assertErrorType(
                "{\"v\":1,\"ts\":123000,\"seq\":42.5}",
                TelemetryMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectStringTemperature() {
        assertErrorType(
                "{\"v\":1,\"ts\":123000,\"seq\":3,\"temp_c\":\"30.2\"}",
                TelemetryMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectStringBoolean() {
        assertErrorType(
                "{\"v\":1,\"ts\":123002,\"seq\":5,\"mqtt_connected\":\"true\"}",
                HealthMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectNumericBoolean() {
        assertErrorType(
                "{\"v\":1,\"ts\":123002,\"seq\":5,\"mqtt_connected\":1}",
                HealthMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectMissingTimestamp() {
        assertErrorType(
                "{\"v\":1,\"seq\":3}",
                TelemetryMessage.class,
                MqttMessageErrorType.CONSTRAINT_VIOLATION
        );
    }

    @Test
    void shouldRejectMissingSequenceNumber() {
        assertErrorType(
                "{\"v\":1,\"ts\":123000}",
                TelemetryMessage.class,
                MqttMessageErrorType.CONSTRAINT_VIOLATION
        );
    }

    @Test
    void shouldRejectWrongJsonValueType() {
        assertErrorType(
                "{\"v\":1,\"ts\":\"invalid\",\"seq\":3}",
                TelemetryMessage.class,
                MqttMessageErrorType.MALFORMED_JSON
        );
    }

    @Test
    void shouldRejectTemperatureOutsideDatabasePrecision() {
        assertErrorType(
                "{\"v\":1,\"ts\":123000,\"seq\":3,\"temp_c\":12345.67}",
                TelemetryMessage.class,
                MqttMessageErrorType.CONSTRAINT_VIOLATION
        );
    }

    @Test
    void shouldRejectBlankEventType() {
        assertErrorType(
                "{\"v\":1,\"ts\":123001,\"seq\":4,\"type\":\" \"}",
                EventMessage.class,
                MqttMessageErrorType.CONSTRAINT_VIOLATION
        );
    }

    @Test
    void shouldRejectEventTypeLongerThanDatabaseColumn() {
        String eventType = "A".repeat(101);

        assertErrorType(
                "{\"v\":1,\"ts\":123001,\"seq\":4,\"type\":\""
                        + eventType
                        + "\"}",
                EventMessage.class,
                MqttMessageErrorType.CONSTRAINT_VIOLATION
        );
    }

    @Test
    void shouldNotIncludePayloadInValidationError() {
        String sensitiveValue = "sensitive-event-value" + "x".repeat(100);

        assertThatThrownBy(() -> parser.parse(
                "{\"v\":1,\"ts\":123001,\"seq\":4,\"type\":\""
                        + sensitiveValue
                        + "\"}",
                EventMessage.class
        ))
                .isInstanceOf(InvalidMqttMessageException.class)
                .hasMessageNotContaining(sensitiveValue);
    }

    private <T extends VersionedMqttMessage>
    void assertErrorType(
            String payload,
            Class<T> targetType,
            MqttMessageErrorType expectedErrorType
    ) {
        assertThatThrownBy(() -> parser.parse(payload, targetType))
                .isInstanceOfSatisfying(
                        InvalidMqttMessageException.class,
                        exception -> assertThat(exception.getErrorType())
                                .isEqualTo(expectedErrorType)
                );
    }
}
