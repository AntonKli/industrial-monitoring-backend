package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.dto.ExportJobResponse;
import com.example.industrialmonitoring.dto.ExportPeriodJobResponse;
import com.example.industrialmonitoring.export.ExportPeriod;
import com.example.industrialmonitoring.service.ExportFileService;
import com.example.industrialmonitoring.service.ExportJobService;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportJobService exportJobService;
    private final ExportFileService exportFileService;
    private final ExportProperties exportProperties;

    public ExportController(
            ExportJobService exportJobService,
            ExportFileService exportFileService,
            ExportProperties exportProperties
    ) {
        this.exportJobService = exportJobService;
        this.exportFileService = exportFileService;
        this.exportProperties = exportProperties;
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
        ExportPeriod period = new ExportPeriod(
                fromDate,
                toDateExclusive,
                exportProperties.zoneId()
        );

        Path finalDirectory =
                exportFileService.finalDirectory(period);

        /*
         * Reuse an existing completed export. This allows the
         * same period to be downloaded repeatedly without
         * starting the same Spring Batch job again.
         */
        if (!Files.isDirectory(finalDirectory)) {
            JobExecution jobExecution =
                    exportJobService.startRangeExport(
                            fromDate,
                            toDateExclusive
                    );

            if (
                    jobExecution.getStatus()
                            != BatchStatus.COMPLETED
            ) {
                throw new IllegalStateException(
                        "Export job did not complete successfully. "
                                + "Current status: "
                                + jobExecution.getStatus()
                );
            }
        }

        if (!Files.isDirectory(finalDirectory)) {
            throw new IllegalStateException(
                    "Completed export directory does not exist: "
                            + finalDirectory
            );
        }

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
}