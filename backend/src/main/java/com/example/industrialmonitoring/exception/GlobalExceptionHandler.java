package com.example.industrialmonitoring.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    public ProblemDetail handleDeviceNotFound(
            DeviceNotFoundException exception
    ) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problemDetail.setTitle("Device not found");
        problemDetail.setDetail(exception.getMessage());

        return problemDetail;
    }

    @ExceptionHandler(TelemetryRecordNotFoundException.class)
    public ProblemDetail handleTelemetryNotFound(
            TelemetryRecordNotFoundException exception
    ) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problemDetail.setTitle("Telemetry not found");
        problemDetail.setDetail(exception.getMessage());

        return problemDetail;
    }

    @ExceptionHandler(HealthRecordNotFoundException.class)
    public ProblemDetail handleHealthNotFound(
            HealthRecordNotFoundException exception
    ) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problemDetail.setTitle("Health not found");
        problemDetail.setDetail(exception.getMessage());

        return problemDetail;
    }

    @ExceptionHandler(InvalidExportYearException.class)
    public ProblemDetail handleInvalidExportYear(
            InvalidExportYearException exception
    ) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problemDetail.setTitle("Invalid export year");
        problemDetail.setDetail(exception.getMessage());

        return problemDetail;
    }
    @ExceptionHandler(InvalidExportPeriodException.class)
public ProblemDetail handleInvalidExportPeriod(
        InvalidExportPeriodException exception
) {
    ProblemDetail problemDetail =
            ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    problemDetail.setTitle("Invalid export period");
    problemDetail.setDetail(exception.getMessage());

    return problemDetail;
}

    @ExceptionHandler(AnnualExportConflictException.class)
    public ProblemDetail handleAnnualExportConflict(
            AnnualExportConflictException exception
    ) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.CONFLICT);

        problemDetail.setTitle("Export conflict");
        problemDetail.setDetail(exception.getMessage());

        return problemDetail;
    }
}