package com.example.industrialmonitoring.scheduler;

import org.springframework.batch.core.BatchStatus;
import com.example.industrialmonitoring.export.ExportPeriod;
import com.example.industrialmonitoring.config.ExportMailProperties;
import com.example.industrialmonitoring.service.ExportMailService;
import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.service.ExportJobService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;


class ExportSchedulerTest {

    @Test
    void shouldStartExportForPreviousYear() {
        ExportJobService exportJobService =
                mock(ExportJobService.class);

        ExportProperties exportProperties =
                createExportProperties();

        ExportMailService exportMailService =
                mock(ExportMailService.class);

        ExportMailProperties exportMailProperties =
                mock(ExportMailProperties.class);

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
                        exportProperties,
                        exportMailService,
                        exportMailProperties
                );

        exportScheduler.exportPreviousYear();

        verify(exportJobService)
                .startAnnualExport(expectedYear);
    }

    @Test
    void shouldSendCompletedAnnualExportByEmail() {
        ExportJobService exportJobService =
                mock(ExportJobService.class);

        ExportProperties exportProperties =
                createExportProperties();

        ExportMailService exportMailService =
                mock(ExportMailService.class);

        ExportMailProperties exportMailProperties =
                mock(ExportMailProperties.class);

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

        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.COMPLETED);

        when(exportMailProperties.enabled())
                .thenReturn(true);

        when(exportMailProperties.annualRecipient())
                .thenReturn("operator@example.com");

        ExportScheduler exportScheduler =
                new ExportScheduler(
                        exportJobService,
                        exportProperties,
                        exportMailService,
                        exportMailProperties
                );

        exportScheduler.exportPreviousYear();

        verify(exportMailService).sendExport(
                argThat(period ->
                        period.fromDate().getYear()
                                == expectedYear
                                && period.fromDate().getMonthValue()
                                == 1
                                && period.fromDate().getDayOfMonth()
                                == 1
                                && period.toDateExclusive().getYear()
                                == expectedYear + 1
                                && period.toDateExclusive().getMonthValue()
                                == 1
                                && period.toDateExclusive().getDayOfMonth()
                                == 1
                ),
                eq("operator@example.com")
        );
    }

    @Test
    void shouldHandleJobStartFailureWithoutCrashingScheduler() {
        ExportJobService exportJobService =
                mock(ExportJobService.class);

        ExportProperties exportProperties =
                createExportProperties();

        ExportMailService exportMailService =
                mock(ExportMailService.class);

        ExportMailProperties exportMailProperties =
                mock(ExportMailProperties.class);

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
                        exportProperties,
                        exportMailService,
                        exportMailProperties
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