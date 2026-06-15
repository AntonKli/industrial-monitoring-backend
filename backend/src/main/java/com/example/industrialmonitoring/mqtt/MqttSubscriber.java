package com.example.industrialmonitoring.mqtt;

import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    private void handleMessage(
            String topic,
            MqttMessage message
    ) {

        String payload = new String(message.getPayload());

        log.info(
                "MQTT message received. topic={} payload={}",
                topic,
                payload
        );

        try {
            mqttMessageDispatcher.dispatch(
                    topic,
                    payload
            );
        } catch (Exception exception) {

            log.error(
                    "Failed to process MQTT message",
                    exception
            );
        }
    }
}