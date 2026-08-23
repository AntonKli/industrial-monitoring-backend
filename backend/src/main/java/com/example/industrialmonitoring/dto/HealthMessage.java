package com.example.industrialmonitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record HealthMessage(

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

        Integer state,

        @JsonProperty("mqtt_connected")
        Boolean mqttConnected,

        @JsonProperty("pub_last_ok")
        Boolean pubLastOk,

        @JsonProperty("buffer_fill")
        Integer bufferFill,

        @JsonProperty("buffer_drops")
        Long bufferDrops,

        @JsonProperty("diag_uptime_s")
        Long diagUptimeS,

        @JsonProperty("diag_reconnects")
        Long diagReconnects,

        @JsonProperty("diag_pub_ok")
        Long diagPubOk,

        @JsonProperty("diag_pub_fail")
        Long diagPubFail,

        @JsonProperty("diag_last_error")
        Integer diagLastError

) implements VersionedMqttMessage {

    public HealthMessage(
            Integer v,
            Long ts,
            Long seq,
            String sessionId,
            Integer state,
            Boolean mqttConnected,
            Boolean pubLastOk,
            Integer bufferFill,
            Long bufferDrops,
            Long diagUptimeS,
            Long diagReconnects,
            Long diagPubOk,
            Long diagPubFail,
            Integer diagLastError
    ) {
        this(
                v,
                ts,
                seq,
                sessionId,
                null,
                state,
                mqttConnected,
                pubLastOk,
                bufferFill,
                bufferDrops,
                diagUptimeS,
                diagReconnects,
                diagPubOk,
                diagPubFail,
                diagLastError
        );
    }
}
