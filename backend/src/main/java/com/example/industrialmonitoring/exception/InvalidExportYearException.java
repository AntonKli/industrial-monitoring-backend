package com.example.industrialmonitoring.exception;

public class InvalidExportYearException
        extends IllegalArgumentException {

    public InvalidExportYearException(String message) {
        super(message);
    }
}