package com.example.industrialmonitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record HealthMessage(

        @NotNull
        Integer v,

        @NotNull
        Long ts,

        @NotNull
        Long seq,

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

) {
}