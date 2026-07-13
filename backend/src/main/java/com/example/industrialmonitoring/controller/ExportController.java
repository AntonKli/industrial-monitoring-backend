package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.dto.ExportJobResponse;
import com.example.industrialmonitoring.service.ExportJobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}