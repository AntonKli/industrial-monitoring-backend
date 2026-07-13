package com.example.industrialmonitoring.service;

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

        exportJobService = new ExportJobService(
                jobLauncher,
                annualMonitoringExportJob
        );
    }

    @Test
    void shouldStartAnnualExportWithYearParameter()
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

        assertEquals(
                2025L,
                parametersCaptor.getValue().getLong("year")
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
        int futureYear =
                Year.now().getValue() + 1;

        InvalidExportYearException exception =
                assertThrows(
                        InvalidExportYearException.class,
                        () -> exportJobService
                                .startAnnualExport(futureYear)
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                String.valueOf(
                                        Year.now().getValue()
                                )
                        )
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