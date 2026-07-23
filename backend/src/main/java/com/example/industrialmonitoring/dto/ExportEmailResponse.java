package com.example.industrialmonitoring.dto;

import java.time.LocalDate;

public record ExportEmailResponse(
        LocalDate fromDate,
        LocalDate toDateExclusive,
        String recipientEmail,
        String status
) {
}