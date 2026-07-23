package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.service.ExportPreparationService;

import com.example.industrialmonitoring.dto.ExportEmailRequest;
import com.example.industrialmonitoring.dto.ExportEmailResponse;
import com.example.industrialmonitoring.service.ExportMailService;
import jakarta.validation.Valid;
import com.example.industrialmonitoring.dto.ExportJobResponse;
import com.example.industrialmonitoring.dto.ExportPeriodJobResponse;
import com.example.industrialmonitoring.export.ExportPeriod;
import com.example.industrialmonitoring.service.ExportFileService;
import com.example.industrialmonitoring.service.ExportJobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportJobService exportJobService;
    private final ExportFileService exportFileService;
    private final ExportPreparationService exportPreparationService;
    private final ExportMailService exportMailService;

    public ExportController(
            ExportJobService exportJobService,
            ExportFileService exportFileService,
            ExportPreparationService exportPreparationService,
            ExportMailService exportMailService
    ) {
        this.exportJobService = exportJobService;
        this.exportFileService = exportFileService;
        this.exportPreparationService = exportPreparationService;
        this.exportMailService = exportMailService;
    }

    @PostMapping("/yearly")
    public ExportJobResponse startYearlyExport(
            @RequestParam int year
    ) {
        JobExecution jobExecution =
                exportJobService.startAnnualExport(year);

        return new ExportJobResponse(
                jobExecution.getId(),
                jobExecution.getJobInstance().getJobName(),
                year,
                jobExecution.getStatus().name()
        );
    }

    @PostMapping("/range")
    public ExportPeriodJobResponse startRangeExport(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDateExclusive
    ) {
        JobExecution jobExecution =
                exportJobService.startRangeExport(
                        fromDate,
                        toDateExclusive
                );

        return new ExportPeriodJobResponse(
                jobExecution.getId(),
                jobExecution.getJobInstance().getJobName(),
                fromDate,
                toDateExclusive,
                jobExecution.getStatus().name()
        );
    }

    @PostMapping(
            value = "/range/download",
            produces = "application/zip"
    )
    public ResponseEntity<StreamingResponseBody> downloadRangeExport(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDateExclusive
    ) {
        ExportPeriod period = exportPreparationService.prepareRangeExport(
                fromDate,
                toDateExclusive
        );

        LocalDate inclusiveToDate =
                toDateExclusive.minusDays(1);

        String fileName =
                "monitoring-export-"
                        + fromDate
                        + "_to_"
                        + inclusiveToDate
                        + ".zip";

        StreamingResponseBody responseBody =
                outputStream ->
                        exportFileService.writeFinalExportAsZip(
                                period,
                                outputStream
                        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + fileName
                                + "\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/zip"
                        )
                )
                .body(responseBody);
    }
    @PostMapping("/range/email")
    public ResponseEntity<ExportEmailResponse> sendRangeExportByEmail(
            @Valid
            @RequestBody
            ExportEmailRequest request
    ) {
        ExportPeriod period =
                exportPreparationService.prepareRangeExport(
                        request.fromDate(),
                        request.toDateExclusive()
                );

        exportMailService.sendExport(
                period,
                request.recipientEmail()
        );

        ExportEmailResponse response =
                new ExportEmailResponse(
                        request.fromDate(),
                        request.toDateExclusive(),
                        request.recipientEmail(),
                        "SENT"
                );

        return ResponseEntity.ok(response);
    }
}