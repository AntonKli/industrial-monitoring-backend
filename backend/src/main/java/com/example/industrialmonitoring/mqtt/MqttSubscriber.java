package com.example.industrialmonitoring.mqtt;

import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

@ConditionalOnProperty(
        name = "mqtt.subscriber.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Component
public class MqttSubscriber {

    private static final Logger log =
            LoggerFactory.getLogger(MqttSubscriber.class);

    private final MqttClient mqttClient;
    private final MqttConnectOptions mqttConnectOptions;
    private final MqttTopicResolver mqttTopicResolver;
    private final MqttMessageDispatcher mqttMessageDispatcher;

    public MqttSubscriber(
            MqttClient mqttClient,
            MqttConnectOptions mqttConnectOptions,
            MqttTopicResolver mqttTopicResolver,
            MqttMessageDispatcher mqttMessageDispatcher
    ) {
        this.mqttClient = mqttClient;
        this.mqttConnectOptions = mqttConnectOptions;
        this.mqttTopicResolver = mqttTopicResolver;
        this.mqttMessageDispatcher = mqttMessageDispatcher;
    }

    @PostConstruct
    public void start() throws Exception {

        mqttClient.connect(mqttConnectOptions);

        mqttClient.subscribe(
                mqttTopicResolver.telemetryTopic(),
                this::handleMessage
        );

        mqttClient.subscribe(
                mqttTopicResolver.eventsTopic(),
                this::handleMessage
        );

        mqttClient.subscribe(
                mqttTopicResolver.healthTopic(),
                this::handleMessage
        );

        log.info("MQTT subscriptions active");
    }

    void handleMessage(
            String topic,
            MqttMessage message
    ) {

        log.info(
                "MQTT message received. topic={}",
                topic
        );

        try {
            String payload = decodePayload(message.getPayload());

            mqttMessageDispatcher.dispatch(
                    topic,
                    payload
            );
        } catch (InvalidMqttMessageException exception) {

            log.warn(
                    "Rejected MQTT message. topic={} errorType={} reason={}",
                    topic,
                    exception.getErrorType(),
                    exception.getMessage()
            );
        } catch (Exception exception) {

            log.error(
                    "Failed to process MQTT message. topic={}",
                    topic,
                    exception
            );
        }
    }

    private String decodePayload(byte[] payload) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(payload))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new InvalidMqttMessageException(
                    MqttMessageErrorType.INVALID_PAYLOAD_ENCODING,
                    "MQTT payload is not valid UTF-8",
                    exception
            );
        }
    }
}
