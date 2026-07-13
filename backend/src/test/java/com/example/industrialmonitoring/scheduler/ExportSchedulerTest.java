package com.example.industrialmonitoring.scheduler;

import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.service.ExportJobService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportSchedulerTest {

    @Test
    void shouldStartExportForPreviousYear() {
        ExportJobService exportJobService =
                mock(ExportJobService.class);

        ExportProperties exportProperties =
                createExportProperties();

        int expectedYear = Year.now(
                        exportProperties.zoneId()
                )
                .minusYears(1)
                .getValue();

        JobExecution jobExecution =
                mock(JobExecution.class);

        when(
                exportJobService.startAnnualExport(expectedYear)
        ).thenReturn(jobExecution);

        ExportScheduler exportScheduler =
                new ExportScheduler(
                        exportJobService,
                        exportProperties
                );

        exportScheduler.exportPreviousYear();

        verify(exportJobService)
                .startAnnualExport(expectedYear);
    }

    @Test
    void shouldHandleJobStartFailureWithoutCrashingScheduler() {
        ExportJobService exportJobService =
                mock(ExportJobService.class);

        ExportProperties exportProperties =
                createExportProperties();

        int expectedYear = Year.now(
                        exportProperties.zoneId()
                )
                .minusYears(1)
                .getValue();

        when(
                exportJobService.startAnnualExport(expectedYear)
        ).thenThrow(
                new IllegalStateException(
                        "Annual export already completed"
                )
        );

        ExportScheduler exportScheduler =
                new ExportScheduler(
                        exportJobService,
                        exportProperties
                );

        assertDoesNotThrow(
                exportScheduler::exportPreviousYear
        );

        verify(exportJobService)
                .startAnnualExport(expectedYear);
    }

    private ExportProperties createExportProperties() {
        return new ExportProperties(
                true,
                "exports",
                100,
                "0 0 2 1 1 *",
                "Europe/Berlin"
        );
    }
}