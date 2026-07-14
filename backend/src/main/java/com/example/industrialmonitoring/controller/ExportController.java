package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.dto.ExportJobResponse;
import com.example.industrialmonitoring.dto.ExportPeriodJobResponse;
import com.example.industrialmonitoring.service.ExportJobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportJobService exportJobService;

    public ExportController(ExportJobService exportJobService) {
        this.exportJobService = exportJobService;
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
}