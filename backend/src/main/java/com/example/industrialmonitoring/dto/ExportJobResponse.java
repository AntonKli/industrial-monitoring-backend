package com.example.industrialmonitoring.dto;

public record ExportJobResponse(
        Long executionId,
        String jobName,
        int year,
        String status
) {
}