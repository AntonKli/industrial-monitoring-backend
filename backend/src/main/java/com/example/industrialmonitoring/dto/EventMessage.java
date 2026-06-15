package com.example.industrialmonitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record EventMessage(
        @NotNull
        Integer v,

        @NotNull
        Long ts,

        @NotNull
        Long seq,

        @NotNull
        @JsonProperty("type")
        String eventType
) {
}