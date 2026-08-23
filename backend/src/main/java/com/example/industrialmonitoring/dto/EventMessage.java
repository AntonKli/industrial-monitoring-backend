package com.example.industrialmonitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record EventMessage(
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

        @NotBlank
        @Size(max = 100)
        @JsonProperty("type")
        String eventType
) implements VersionedMqttMessage {

    public EventMessage(
            Integer v,
            Long ts,
            Long seq,
            String sessionId,
            String eventType
    ) {
        this(v, ts, seq, sessionId, null, eventType);
    }
}
