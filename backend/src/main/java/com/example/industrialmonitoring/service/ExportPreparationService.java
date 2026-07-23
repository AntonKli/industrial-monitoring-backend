package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.export.ExportPeriod;
import org.springframework.stereotype.Service;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Service
public class ExportPreparationService {

    private final ExportJobService exportJobService;
    private final ExportFileService exportFileService;
    private final ExportProperties exportProperties;

    public ExportPreparationService(
            ExportJobService exportJobService,
            ExportFileService exportFileService,
            ExportProperties exportProperties
    ) {
        this.exportJobService = exportJobService;
        this.exportFileService = exportFileService;
        this.exportProperties = exportProperties;
    }

    public ExportPeriod prepareRangeExport(
            LocalDate fromDate,
            LocalDate toDateExclusive
    ) {
        ExportPeriod period = new ExportPeriod(
                fromDate,
                toDateExclusive,
                exportProperties.zoneId()
        );

        Path finalDirectory = exportFileService.finalDirectory(period);

        if (Files.isDirectory(finalDirectory)) {
            return period;
        }

        JobExecution jobExecution = exportJobService.startRangeExport(
                fromDate,
                toDateExclusive
        );

        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Export job did not complete successfully. "
                            + "Current status: "
                            + jobExecution.getStatus()
            );
        }

        if (!Files.isDirectory(finalDirectory)) {
            throw new IllegalStateException(
                    "Completed export directory does not exist: "
                            + finalDirectory
            );
        }

        return period;
    }
}