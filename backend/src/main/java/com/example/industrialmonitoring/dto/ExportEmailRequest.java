package com.example.industrialmonitoring.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExportEmailRequest(

        @NotNull(message = "Export start date is required")
        LocalDate fromDate,

        @NotNull(message = "Exclusive export end date is required")
        LocalDate toDateExclusive,

        @NotBlank(message = "Recipient email address is required")
        @Email(message = "Recipient email address is invalid")
        String recipientEmail
) {
}