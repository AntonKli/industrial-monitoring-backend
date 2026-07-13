package com.example.industrialmonitoring.exception;

public class AnnualExportConflictException
        extends IllegalStateException {

    public AnnualExportConflictException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}