package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.exception.AnnualExportConflictException;
import com.example.industrialmonitoring.exception.InvalidExportYearException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        ExportProperties exportProperties = new ExportProperties(
                false,
                "exports",
                100,
                "0 0 2 1 1 *",
                "Europe/Berlin"
        );

        exportJobService = new ExportJobService(
                jobLauncher,
                annualMonitoringExportJob,
                exportProperties
        );
    }

    @Test
    void shouldStartAnnualExportWithPeriodParameters()
            throws Exception {

        JobExecution jobExecution =
                mock(JobExecution.class);

        when(
                jobLauncher.run(
                        eq(annualMonitoringExportJob),
                        any(JobParameters.class)
                )
        ).thenReturn(jobExecution);

        JobExecution result =
                exportJobService.startAnnualExport(2025);

        ArgumentCaptor<JobParameters> parametersCaptor =
                ArgumentCaptor.forClass(JobParameters.class);

        verify(jobLauncher).run(
                eq(annualMonitoringExportJob),
                parametersCaptor.capture()
        );

        JobParameters parameters =
                parametersCaptor.getValue();

        assertEquals(
                2025L,
                parameters.getLong("year")
        );

        assertEquals(
                "2025-01-01",
                parameters.getString("fromDate")
        );

        assertEquals(
                "2026-01-01",
                parameters.getString("toDateExclusive")
        );

        assertEquals(
                "Europe/Berlin",
                parameters.getString("zoneId")
        );

        assertTrue(
                parameters.getParameter("year")
                        .isIdentifying()
        );

        assertFalse(
                parameters.getParameter("fromDate")
                        .isIdentifying()
        );

        assertFalse(
                parameters.getParameter("toDateExclusive")
                        .isIdentifying()
        );

        assertFalse(
                parameters.getParameter("zoneId")
                        .isIdentifying()
        );

        assertSame(jobExecution, result);
    }

    @Test
    void shouldRejectYearBeforeMinimum() {
        InvalidExportYearException exception =
                assertThrows(
                        InvalidExportYearException.class,
                        () -> exportJobService
                                .startAnnualExport(1999)
                );

        assertTrue(
                exception.getMessage()
                        .contains("between 2000")
        );

        verifyNoInteractions(jobLauncher);
    }

    @Test
    void shouldRejectFutureYear() {
        int currentYear = Year.now().getValue();
        int futureYear = currentYear + 1;

        InvalidExportYearException exception =
                assertThrows(
                        InvalidExportYearException.class,
                        () -> exportJobService
                                .startAnnualExport(futureYear)
                );

        assertTrue(
                exception.getMessage()
                        .contains(String.valueOf(currentYear))
        );

        verifyNoInteractions(jobLauncher);
    }

    @Test
    void shouldTranslateCompletedJobIntoConflictException()
            throws Exception {

        when(
                jobLauncher.run(
                        eq(annualMonitoringExportJob),
                        any(JobParameters.class)
                )
        ).thenThrow(
                new JobInstanceAlreadyCompleteException(
                        "Job instance already completed"
                )
        );

        AnnualExportConflictException exception =
                assertThrows(
                        AnnualExportConflictException.class,
                        () -> exportJobService
                                .startAnnualExport(2022)
                );

        assertTrue(
                exception.getMessage().contains("2022")
        );

        assertTrue(
                exception.getCause()
                        instanceof JobInstanceAlreadyCompleteException
        );
    }

    @Test
    void shouldTreatInvalidBatchParametersAsTechnicalFailure()
            throws Exception {

        when(
                jobLauncher.run(
                        eq(annualMonitoringExportJob),
                        any(JobParameters.class)
                )
        ).thenThrow(
                new JobParametersInvalidException(
                        "Invalid job parameters"
                )
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> exportJobService
                                .startAnnualExport(2021)
                );

        assertTrue(
                exception.getMessage().contains("2021")
        );

        assertTrue(
                exception.getCause()
                        instanceof JobParametersInvalidException
        );
    }
}