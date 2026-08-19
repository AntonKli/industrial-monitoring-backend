package com.example.industrialmonitoring.mqtt;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MqttSubscriberTest {

    private MqttMessageDispatcher dispatcher;
    private MqttSubscriber subscriber;

    @BeforeEach
    void setUp() {
        dispatcher = mock(MqttMessageDispatcher.class);
        subscriber = new MqttSubscriber(
                mock(MqttClient.class),
                new MqttConnectOptions(),
                mock(MqttTopicResolver.class),
                dispatcher
        );
    }

    @Test
    void shouldRejectInvalidUtf8AsExpectedMqttErrorBeforeDispatch() {
        Logger logger = (Logger) LoggerFactory.getLogger(MqttSubscriber.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            subscriber.handleMessage(
                    "rtz/edge01/events",
                    new MqttMessage(invalidUtf8EventPayload())
            );

            verifyNoInteractions(dispatcher);

            assertThat(appender.list)
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        assertThat(event.getFormattedMessage())
                                .contains("errorType=INVALID_PAYLOAD_ENCODING")
                                .doesNotContain("sensitive-value");
                    })
                    .noneMatch(event -> event.getLevel() == Level.ERROR);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private byte[] invalidUtf8EventPayload() {
        byte[] prefix = (
                "{\"v\":1,\"ts\":123001,\"seq\":4,"
                        + "\"type\":\"sensitive-value"
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
