package com.example.industrialmonitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TelemetryMessage(
        @NotNull
        Integer v,

        @NotNull
        Long ts,

        @NotNull
        Long seq,

        @JsonProperty("temp_c")
        BigDecimal temperatureC,

        Integer rpm
) {
}