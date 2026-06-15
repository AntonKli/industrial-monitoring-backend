package com.example.industrialmonitoring.exception;

public class TelemetryRecordNotFoundException extends RuntimeException {

    public TelemetryRecordNotFoundException() {
        super("No telemetry records found");
    }
}