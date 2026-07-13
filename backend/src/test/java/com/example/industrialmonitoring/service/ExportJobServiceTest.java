package com.example.industrialmonitoring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExportJobServiceTest {

    private JobLauncher jobLauncher;
    private Job annualMonitoringExportJob;
    private ExportJobService exportJobService;

    @BeforeEach
    void setUp() {
        jobLauncher = mock(JobLauncher.class);
        annualMonitoringExportJob = mock(Job.class);

        exportJobService = new ExportJobService(
                jobLauncher,
                annualMonitoringExportJob
        );
    }

    @Test
    void shouldStartAnnualExportWithYearParameter() throws Exception {
        int exportYear = 2020;
        JobExecution expectedExecution = mock(JobExecution.class);

        when(
                jobLauncher.run(
                        eq(annualMonitoringExportJob),
                        any(JobParameters.class)
                )
        ).thenReturn(expectedExecution);

        JobExecution actualExecution =
                exportJobService.startAnnualExport(exportYear);

        ArgumentCaptor<JobParameters> parametersCaptor =
                ArgumentCaptor.forClass(JobParameters.class);

        verify(jobLauncher).run(
                eq(annualMonitoringExportJob),
                parametersCaptor.capture()
        );

        JobParameters capturedParameters =
                parametersCaptor.getValue();

        assertEquals(
                Long.valueOf(exportYear),
                capturedParameters.getLong("year")
        );

        assertSame(
                expectedExecution,
                actualExecution
        );
    }

    @Test
    void shouldRejectFutureExportYear() {
        int currentYear = Year.now().getValue();
        int futureYear = currentYear + 1;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> exportJobService.startAnnualExport(futureYear)
        );

        assertEquals(
                "Export year must be between 2000 and " + currentYear,
                exception.getMessage()
        );

        verifyNoInteractions(jobLauncher);
    }

    @Test
    void shouldRejectExportYearBefore2000() {
        int currentYear = Year.now().getValue();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> exportJobService.startAnnualExport(1999)
        );

        assertEquals(
                "Export year must be between 2000 and " + currentYear,
                exception.getMessage()
        );

        verifyNoInteractions(jobLauncher);
    }
}