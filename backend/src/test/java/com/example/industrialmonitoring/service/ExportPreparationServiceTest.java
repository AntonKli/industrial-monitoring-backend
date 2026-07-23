package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.export.ExportPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportPreparationServiceTest {

    private ExportJobService exportJobService;
    private ExportFileService exportFileService;
    private ExportProperties exportProperties;
    private ExportPreparationService exportPreparationService;

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        exportJobService = mock(ExportJobService.class);
        exportFileService = mock(ExportFileService.class);
        exportProperties = mock(ExportProperties.class);

        exportPreparationService =
                new ExportPreparationService(
                        exportJobService,
                        exportFileService,
                        exportProperties
                );
    }

    @Test
    void shouldStartNewExportWhenNoCompletedExportExists() {
        LocalDate fromDate =
                LocalDate.of(2025, 1, 1);

        LocalDate toDateExclusive =
                LocalDate.of(2026, 1, 1);

        Path finalDirectory =
                temporaryDirectory.resolve("completed-export");

        JobExecution jobExecution =
                mock(JobExecution.class);

        when(exportProperties.zoneId())
                .thenReturn(
                        ZoneId.of("Europe/Berlin")
                );

        when(
                exportFileService.finalDirectory(
                        any(ExportPeriod.class)
                )
        ).thenReturn(finalDirectory);

        when(
                exportJobService.startRangeExport(
                        fromDate,
                        toDateExclusive
                )
        ).thenAnswer(invocation -> {
            java.nio.file.Files.createDirectories(
                    finalDirectory
            );

            return jobExecution;
        });

        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.COMPLETED);

        ExportPeriod result =
                exportPreparationService.prepareRangeExport(
                        fromDate,
                        toDateExclusive
                );

        assertNotNull(result);

        verify(exportJobService)
                .startRangeExport(
                        fromDate,
                        toDateExclusive
                );
    }

    @Test
    void shouldReuseExistingExportWithoutStartingNewJob() {
        LocalDate fromDate =
                LocalDate.of(2025, 1, 1);

        LocalDate toDateExclusive =
                LocalDate.of(2026, 1, 1);

        when(exportProperties.zoneId())
                .thenReturn(
                        ZoneId.of("Europe/Berlin")
                );

        when(
                exportFileService.finalDirectory(
                        any(ExportPeriod.class)
                )
        ).thenReturn(temporaryDirectory);

        ExportPeriod result =
                exportPreparationService.prepareRangeExport(
                        fromDate,
                        toDateExclusive
                );

        assertNotNull(result);

        verify(exportFileService)
                .finalDirectory(result);

        verify(exportJobService, never())
                .startRangeExport(
                        any(LocalDate.class),
                        any(LocalDate.class)
                );
    }
}