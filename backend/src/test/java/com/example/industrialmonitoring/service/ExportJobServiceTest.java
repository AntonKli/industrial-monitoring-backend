package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.exception.InvalidExportPeriodException;
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
import java.time.ZoneId;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private static final ZoneId BERLIN =
            ZoneId.of("Europe/Berlin");

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
                BERLIN.getId()
        );

        exportJobService = new ExportJobService(
                jobLauncher,
                annualMonitoringExportJob,
                exportProperties
        );
    }
    @Test
void shouldStartRangeExportWithIdentifyingPeriodParameters()
        throws Exception {

    LocalDate fromDate =
            LocalDate.of(2025, 10, 1);

    LocalDate toDateExclusive =
            LocalDate.of(2026, 4, 1);

    JobExecution jobExecution =
            mock(JobExecution.class);

    when(
            jobLauncher.run(
                    eq(annualMonitoringExportJob),
                    any(JobParameters.class)
            )
    ).thenReturn(jobExecution);

    JobExecution result =
            exportJobService.startRangeExport(
                    fromDate,
                    toDateExclusive
            );

    ArgumentCaptor<JobParameters> parametersCaptor =
            ArgumentCaptor.forClass(JobParameters.class);

    verify(jobLauncher).run(
            eq(annualMonitoringExportJob),
            parametersCaptor.capture()
    );

    JobParameters parameters =
            parametersCaptor.getValue();

    assertNull(parameters.getParameter("year"));

    assertEquals(
            "2025-10-01",
            parameters.getString("fromDate")
    );

    assertEquals(
            "2026-04-01",
            parameters.getString("toDateExclusive")
    );

    assertEquals(
            BERLIN.getId(),
            parameters.getString("zoneId")
    );

    assertTrue(
            parameters.getParameter("fromDate")
                    .isIdentifying()
    );

    assertTrue(
            parameters.getParameter("toDateExclusive")
                    .isIdentifying()
    );

    assertTrue(
            parameters.getParameter("zoneId")
                    .isIdentifying()
    );

    assertSame(jobExecution, result);
}
    @Test
    void shouldStartAnnualExportWithIdentifyingPeriodParameters()
            throws Exception {

        int completedYear =
                Year.now(BERLIN).getValue() - 1;

        JobExecution jobExecution =
                mock(JobExecution.class);

        when(
                jobLauncher.run(
                        eq(annualMonitoringExportJob),
                        any(JobParameters.class)
                )
        ).thenReturn(jobExecution);

        JobExecution result =
                exportJobService.startAnnualExport(completedYear);

        ArgumentCaptor<JobParameters> parametersCaptor =
                ArgumentCaptor.forClass(JobParameters.class);

        verify(jobLauncher).run(
                eq(annualMonitoringExportJob),
                parametersCaptor.capture()
        );

        JobParameters parameters =
                parametersCaptor.getValue();

        assertNull(parameters.getParameter("year"));

        assertEquals(
                completedYear + "-01-01",
                parameters.getString("fromDate")
        );

        assertEquals(
                completedYear + 1 + "-01-01",
                parameters.getString("toDateExclusive")
        );

        assertEquals(
                BERLIN.getId(),
                parameters.getString("zoneId")
        );

        assertTrue(
                parameters.getParameter("fromDate")
                        .isIdentifying()
        );

        assertTrue(
                parameters.getParameter("toDateExclusive")
                        .isIdentifying()
        );

        assertTrue(
                parameters.getParameter("zoneId")
                        .isIdentifying()
        );

        assertSame(jobExecution, result);
    }
    @Test
void shouldRejectEmptyRange() {
    LocalDate date =
            LocalDate.of(2026, 4, 1);

    InvalidExportPeriodException exception =
            assertThrows(
                    InvalidExportPeriodException.class,
                    () -> exportJobService.startRangeExport(
                            date,
                            date
                    )
            );

    assertTrue(
            exception.getMessage()
                    .contains("must be before")
    );

    verifyNoInteractions(jobLauncher);
}

@Test
void shouldRejectReversedRange() {
    InvalidExportPeriodException exception =
            assertThrows(
                    InvalidExportPeriodException.class,
                    () -> exportJobService.startRangeExport(
                            LocalDate.of(2026, 4, 2),
                            LocalDate.of(2026, 4, 1)
                    )
            );

    assertTrue(
            exception.getMessage()
                    .contains("must be before")
    );

    verifyNoInteractions(jobLauncher);
}

@Test
void shouldRejectRangeBeforeMinimumDate() {
    InvalidExportPeriodException exception =
            assertThrows(
                    InvalidExportPeriodException.class,
                    () -> exportJobService.startRangeExport(
                            LocalDate.of(1999, 12, 31),
                            LocalDate.of(2000, 1, 2)
                    )
            );

    assertTrue(
            exception.getMessage()
                    .contains("2000-01-01")
    );

    verifyNoInteractions(jobLauncher);
}

@Test
void shouldRejectRangeEndingTooFarInFuture() {
    LocalDate latestAllowedEnd =
            LocalDate.now(BERLIN).plusDays(1);

    InvalidExportPeriodException exception =
            assertThrows(
                    InvalidExportPeriodException.class,
                    () -> exportJobService.startRangeExport(
                            latestAllowedEnd.minusDays(1),
                            latestAllowedEnd.plusDays(1)
                    )
            );

    assertTrue(
            exception.getMessage()
                    .contains("must not be after")
    );

    verifyNoInteractions(jobLauncher);
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
    void shouldRejectCurrentYear() {
        int currentYear =
                Year.now(BERLIN).getValue();

        InvalidExportYearException exception =
                assertThrows(
                        InvalidExportYearException.class,
                        () -> exportJobService
                                .startAnnualExport(currentYear)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Use a range export")
        );

        verifyNoInteractions(jobLauncher);
    }

    @Test
    void shouldRejectFutureYear() {
        int futureYear =
                Year.now(BERLIN).getValue() + 1;

        assertThrows(
                InvalidExportYearException.class,
                () -> exportJobService
                        .startAnnualExport(futureYear)
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