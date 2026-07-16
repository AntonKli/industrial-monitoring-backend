package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.service.ExportFileService;
import com.example.industrialmonitoring.exception.AnnualExportConflictException;
import com.example.industrialmonitoring.exception.GlobalExceptionHandler;
import com.example.industrialmonitoring.exception.InvalidExportPeriodException;
import com.example.industrialmonitoring.exception.InvalidExportYearException;
import com.example.industrialmonitoring.service.ExportJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExportControllerTest {

    private ExportJobService exportJobService;
    private ExportFileService exportFileService;
    private ExportProperties exportProperties;
    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {
        exportJobService = mock(ExportJobService.class);
        exportFileService = mock(ExportFileService.class);
        exportProperties = mock(ExportProperties.class);

        ExportController exportController =
                new ExportController(
                        exportJobService,
                        exportFileService,
                        exportProperties
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(exportController)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }
    @Test
    void shouldStartRangeExport()
            throws Exception {

        LocalDate fromDate =
                LocalDate.of(2025, 10, 1);

        LocalDate toDateExclusive =
                LocalDate.of(2026, 4, 1);

        JobExecution jobExecution =
                mock(JobExecution.class);

        JobInstance jobInstance =
                mock(JobInstance.class);

        when(
                exportJobService.startRangeExport(
                        fromDate,
                        toDateExclusive
                )
        ).thenReturn(jobExecution);

        when(jobExecution.getId())
                .thenReturn(42L);

        when(jobExecution.getJobInstance())
                .thenReturn(jobInstance);

        when(jobInstance.getJobName())
                .thenReturn("annualMonitoringExportJob");

        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.COMPLETED);

        mockMvc.perform(
                        post("/api/exports/range")
                                .param("from", "2025-10-01")
                                .param("to", "2026-04-01")
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                "application/json"
                        )
                )
                .andExpect(
                        jsonPath("$.executionId").value(42)
                )
                .andExpect(
                        jsonPath("$.jobName")
                                .value("annualMonitoringExportJob")
                )
                .andExpect(
                        jsonPath("$.fromDate")
                                .value("2025-10-01")
                )
                .andExpect(
                        jsonPath("$.toDateExclusive")
                                .value("2026-04-01")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("COMPLETED")
                );
    }

    @Test
    void shouldReturnBadRequestForInvalidExportPeriod()
            throws Exception {

        LocalDate date =
                LocalDate.of(2026, 4, 1);

        when(
                exportJobService.startRangeExport(
                        date,
                        date
                )
        ).thenThrow(
                new InvalidExportPeriodException(
                        "Export start date must be before "
                                + "the exclusive end date"
                )
        );

        mockMvc.perform(
                        post("/api/exports/range")
                                .param("from", "2026-04-01")
                                .param("to", "2026-04-01")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentType(
                                "application/problem+json"
                        )
                )
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Invalid export period")
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Export start date must be "
                                                + "before the exclusive "
                                                + "end date"
                                )
                );
    }

    @Test
    void shouldReturnBadRequestForInvalidExportYear()
            throws Exception {

        String detail =
                "Export year must be between 2000 and 2025. "
                        + "Use a range export for the current year.";

        when(exportJobService.startAnnualExport(2026))
                .thenThrow(
                        new InvalidExportYearException(detail)
                );

        mockMvc.perform(
                        post("/api/exports/yearly")
                                .param("year", "2026")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentType(
                                "application/problem+json"
                        )
                )
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Invalid export year")
                )
                .andExpect(
                        jsonPath("$.detail").value(detail)
                );
    }

    @Test
    void shouldReturnConflictForCompletedOrRunningExport()
            throws Exception {

        String detail =
                "Export for period 2022 is already running, "
                        + "completed or cannot currently be restarted";

        when(exportJobService.startAnnualExport(2022))
                .thenThrow(
                        new AnnualExportConflictException(
                                detail,
                                new IllegalStateException(
                                        "Job instance already completed"
                                )
                        )
                );

        mockMvc.perform(
                        post("/api/exports/yearly")
                                .param("year", "2022")
                )
                .andExpect(status().isConflict())
                .andExpect(
                        content().contentType(
                                "application/problem+json"
                        )
                )
                .andExpect(
                        jsonPath("$.status").value(409)
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Export conflict")
                )
                .andExpect(
                        jsonPath("$.detail").value(detail)
                );
    }
}