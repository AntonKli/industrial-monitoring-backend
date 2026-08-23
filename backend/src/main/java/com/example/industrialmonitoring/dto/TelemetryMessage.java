package com.example.industrialmonitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record TelemetryMessage(
        @NotNull
        Integer v,

        @NotNull
        Long ts,

        @NotNull
        @PositiveOrZero
        Long seq,

        @NotNull
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        @JsonProperty("session_id")
        String sessionId,

        @JsonProperty("session_generation")
        Long sessionGeneration,

        @JsonProperty("temp_c")
        @Digits(integer = 4, fraction = 2)
        BigDecimal temperatureC,

        Integer rpm
) implements VersionedMqttMessage {

    public TelemetryMessage(
            Integer v,
            Long ts,
            Long seq,
            String sessionId,
            BigDecimal temperatureC,
            Integer rpm
    ) {
        this(v, ts, seq, sessionId, null, temperatureC, rpm);
    }
}
