package com.example.industrialmonitoring.mqtt;

import com.example.industrialmonitoring.dto.VersionedMqttMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MqttMessageParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public MqttMessageParser(
            ObjectMapper objectMapper,
            Validator validator
    ) {
        this.objectMapper = strictMqttObjectMapper(objectMapper);
        this.validator = validator;
    }

    public <T extends VersionedMqttMessage> T parse(
            String payload,
            Class<T> targetType
    ) {
        if (payload == null || payload.isBlank()) {
            throw malformedJson("MQTT payload must not be blank", null);
        }

        try {
            T message = objectMapper.readValue(payload, targetType);

            if (message == null) {
                throw malformedJson(
                        "MQTT payload must contain a JSON object",
                        null
                );
            }

            validateProtocolVersion(message);
            validateConstraints(message);
            validateSessionGeneration(message);

            return message;
        } catch (JsonProcessingException exception) {
            throw malformedJson("Failed to parse MQTT payload", exception);
        }
    }

    private void validateProtocolVersion(VersionedMqttMessage message) {
        if (message.v() != null && message.v() != 2 && message.v() != 3) {
            throw new InvalidMqttMessageException(
                    MqttMessageErrorType.UNSUPPORTED_PROTOCOL_VERSION,
                    "Unsupported MQTT protocol version"
            );
        }
    }

    private void validateSessionGeneration(VersionedMqttMessage message) {
        if (message.v() == null || message.v() != 3) {
            return;
        }

        if (message.sessionGeneration() == null || message.sessionGeneration() < 1) {
            throw new InvalidMqttMessageException(
                    MqttMessageErrorType.CONSTRAINT_VIOLATION,
                    "MQTT protocol v3 requires session_generation between 1 and Long.MAX_VALUE"
            );
        }
    }

    private <T> void validateConstraints(T message) {
        Set<ConstraintViolation<T>> violations = validator.validate(message);

        if (violations.isEmpty()) {
            return;
        }

        String violationDetails = violations.stream()
                .map(violation ->
                        violation.getPropertyPath()
                                + ": "
                                + violation.getMessage()
                )
                .sorted()
                .collect(Collectors.joining(", "));

        throw new InvalidMqttMessageException(
                MqttMessageErrorType.CONSTRAINT_VIOLATION,
                "MQTT payload violates constraints: " + violationDetails
        );
    }

    private InvalidMqttMessageException malformedJson(
            String message,
            Throwable cause
    ) {
        return new InvalidMqttMessageException(
                MqttMessageErrorType.MALFORMED_JSON,
                message,
                cause
        );
    }

    private ObjectMapper strictMqttObjectMapper(ObjectMapper source) {
        ObjectMapper strictMapper = source.copy();

        strictMapper.coercionConfigFor(LogicalType.Integer)
                .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);

        strictMapper.coercionConfigFor(LogicalType.Float)
                .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);

        strictMapper.coercionConfigFor(LogicalType.Boolean)
                .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail);

        strictMapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);

        return strictMapper;
    }
}
