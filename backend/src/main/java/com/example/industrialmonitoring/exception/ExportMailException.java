package com.example.industrialmonitoring.exception;

public class ExportMailException extends RuntimeException {

    public ExportMailException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}