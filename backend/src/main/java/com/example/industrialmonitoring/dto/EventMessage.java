package com.example.industrialmonitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventMessage(
        @NotNull
        Integer v,

        @NotNull
        Long ts,

        @NotNull
        Long seq,

        @NotBlank
        @Size(max = 100)
        @JsonProperty("type")
        String eventType
) implements VersionedMqttMessage {
}
