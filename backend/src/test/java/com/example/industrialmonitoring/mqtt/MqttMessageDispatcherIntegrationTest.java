package com.example.industrialmonitoring.mqtt;

import com.example.industrialmonitoring.repository.DeviceRepository;
import com.example.industrialmonitoring.repository.TelemetryRecordRepository;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Testcontainers
class MqttMessageDispatcherIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("industrial_monitoring_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("mqtt.subscriber.enabled", () -> false);
        registry.add("mqtt.broker-url", () -> "tcp://localhost:1883");
        registry.add("mqtt.client-id", () -> "test-client-dispatcher");
        registry.add("mqtt.topic-root", () -> "rtz");
        registry.add("mqtt.device-id", () -> "edge01");
        registry.add("mqtt.username", () -> "edge");
        registry.add("mqtt.password", () -> "edge_password");
    }

    @Autowired
    private MqttMessageDispatcher dispatcher;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TelemetryRecordRepository telemetryRecordRepository;

    @BeforeEach
    void setUp() {
        telemetryRecordRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    void shouldPersistValidMessageFromTopicAndJson() {
        dispatcher.dispatch(
                "rtz/edge01/telemetry",
                "{\"v\":1,\"ts\":123000,\"seq\":3,\"temp_c\":30.2,\"rpm\":1600}"
        );

        assertThat(deviceRepository.existsByDeviceId("edge01")).isTrue();
        assertThat(telemetryRecordRepository.findAll())
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getDeviceId()).isEqualTo("edge01");
                    assertThat(record.getGatewayTimestamp()).isEqualTo(123000L);
                    assertThat(record.getSequenceNumber()).isEqualTo(3L);
                    assertThat(record.getTemperatureC()).isEqualByComparingTo("30.2");
                    assertThat(record.getRpm()).isEqualTo(1600);
                });
    }

    @Test
    void shouldNotPersistDeviceOrRecordForInvalidMessage() {
        assertThatThrownBy(() -> dispatcher.dispatch(
                "rtz/edge01/telemetry",
                "{\"v\":2,\"ts\":123000,\"seq\":3}"
        )).isInstanceOfSatisfying(
                InvalidMqttMessageException.class,
                exception -> assertThat(exception.getErrorType())
                        .isEqualTo(MqttMessageErrorType.UNSUPPORTED_PROTOCOL_VERSION)
        );

        assertThat(deviceRepository.count()).isZero();
        assertThat(telemetryRecordRepository.count()).isZero();
    }

    @Test
    void shouldNotPersistDeviceOrRecordForInvalidUtf8Payload() {
        MqttSubscriber subscriber = new MqttSubscriber(
                mock(MqttClient.class),
                new MqttConnectOptions(),
                mock(MqttTopicResolver.class),
                dispatcher
        );

        subscriber.handleMessage(
                "rtz/edge01/telemetry",
                new MqttMessage(invalidUtf8TelemetryPayload())
        );

        assertThat(deviceRepository.count()).isZero();
        assertThat(telemetryRecordRepository.count()).isZero();
    }

    private byte[] invalidUtf8TelemetryPayload() {
        byte[] prefix = (
                "{\"v\":1,\"ts\":123000,\"seq\":3,\"note\":\""
        ).getBytes(StandardCharsets.UTF_8);
        byte[] suffix = "\"}".getBytes(StandardCharsets.UTF_8);
        byte[] payload = Arrays.copyOf(
                prefix,
                prefix.length + 2 + suffix.length
        );

        payload[prefix.length] = (byte) 0xC3;
        payload[prefix.length + 1] = (byte) 0x28;
        System.arraycopy(
                suffix,
                0,
                payload,
                prefix.length + 2,
                suffix.length
        );

        return payload;
    }
}
