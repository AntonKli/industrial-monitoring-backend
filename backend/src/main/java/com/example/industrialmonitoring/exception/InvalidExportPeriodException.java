package com.example.industrialmonitoring.exception;

public class InvalidExportPeriodException
        extends RuntimeException {

    public InvalidExportPeriodException(String message) {
        super(message);
    }
}