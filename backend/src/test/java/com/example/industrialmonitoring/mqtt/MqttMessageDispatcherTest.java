package com.example.industrialmonitoring.mqtt;

import com.example.industrialmonitoring.config.MqttProperties;
import com.example.industrialmonitoring.dto.EventMessage;
import com.example.industrialmonitoring.dto.HealthMessage;
import com.example.industrialmonitoring.dto.TelemetryMessage;
import com.example.industrialmonitoring.service.MqttIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MqttMessageDispatcherTest {

    private MqttIngestionService ingestionService;
    private MqttMessageDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        MqttProperties properties = new MqttProperties(
                "tcp://localhost:1883",
                "test-client",
                "rtz",
                "edge01",
                "edge",
                "password"
        );

        ingestionService = mock(MqttIngestionService.class);
        dispatcher = new MqttMessageDispatcher(
                new MqttTopicParser(properties),
                new MqttMessageParser(
                        new ObjectMapper(),
                        Validation.buildDefaultValidatorFactory().getValidator()
                ),
                ingestionService
        );
    }

    @Test
    void shouldDispatchValidTelemetryMessage() {
        dispatcher.dispatch(
                "rtz/edge01/telemetry",
                "{\"v\":1,\"ts\":123000,\"seq\":3,\"temp_c\":30.2,\"rpm\":1600}"
        );

        verify(ingestionService).ingestTelemetry(
                eq("edge01"),
                eq(new TelemetryMessage(
                        1,
                        123000L,
                        3L,
                        new java.math.BigDecimal("30.2"),
                        1600
                ))
        );
    }

    @Test
    void shouldDispatchValidEventMessage() {
        dispatcher.dispatch(
                "rtz/edge01/events",
                "{\"v\":1,\"ts\":123001,\"seq\":4,\"type\":\"ALARM_RAISED\"}"
        );

        verify(ingestionService).ingestEvent(
                eq("edge01"),
                eq(new EventMessage(1, 123001L, 4L, "ALARM_RAISED"))
        );
    }

    @Test
    void shouldDispatchValidHealthMessage() {
        dispatcher.dispatch(
                "rtz/edge01/health",
                "{\"v\":1,\"ts\":123002,\"seq\":5,\"mqtt_connected\":true}"
        );

        verify(ingestionService).ingestHealth(
                eq("edge01"),
                eq(new HealthMessage(
                        1,
                        123002L,
                        5L,
                        null,
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        );
    }

    @Test
    void shouldNotCallIngestionForInvalidPayload() {
        assertThatThrownBy(() -> dispatcher.dispatch(
                "rtz/edge01/telemetry",
                "{\"v\":2,\"ts\":123000,\"seq\":3}"
        )).isInstanceOf(InvalidMqttMessageException.class);

        verify(ingestionService, never())
                .ingestTelemetry(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(TelemetryMessage.class)
                );
        verify(ingestionService, never())
                .ingestEvent(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(EventMessage.class)
                );
        verify(ingestionService, never())
                .ingestHealth(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(HealthMessage.class)
                );
    }

    @Test
    void shouldNotCallIngestionForInvalidTopic() {
        assertThatThrownBy(() -> dispatcher.dispatch(
                "other/edge01/telemetry",
                "{\"v\":1,\"ts\":123000,\"seq\":3}"
        )).isInstanceOf(InvalidMqttMessageException.class);

        verify(ingestionService, never())
                .ingestTelemetry(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(TelemetryMessage.class)
                );
    }
}
