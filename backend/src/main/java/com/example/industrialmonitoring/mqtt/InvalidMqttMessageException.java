package com.example.industrialmonitoring.mqtt;

public class InvalidMqttMessageException extends IllegalArgumentException {

    private final MqttMessageErrorType errorType;

    public InvalidMqttMessageException(
            MqttMessageErrorType errorType,
            String message
    ) {
        super(message);
        this.errorType = errorType;
    }

    public InvalidMqttMessageException(
            MqttMessageErrorType errorType,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorType = errorType;
    }

    public MqttMessageErrorType getErrorType() {
        return errorType;
    }
}
