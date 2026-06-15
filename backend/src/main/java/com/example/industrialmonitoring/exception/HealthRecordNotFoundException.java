package com.example.industrialmonitoring.exception;

public class HealthRecordNotFoundException extends RuntimeException {

    public HealthRecordNotFoundException() {
        super("No health records found");
    }
}