package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.exception.AnnualExportConflictException;
import com.example.industrialmonitoring.exception.GlobalExceptionHandler;
import com.example.industrialmonitoring.exception.InvalidExportYearException;
import com.example.industrialmonitoring.service.ExportJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExportControllerTest {

    private ExportJobService exportJobService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        exportJobService = mock(ExportJobService.class);

        ExportController exportController =
                new ExportController(exportJobService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(exportController)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void shouldReturnBadRequestForInvalidExportYear()
            throws Exception {

        when(exportJobService.startAnnualExport(1999))
                .thenThrow(
                        new InvalidExportYearException(
                                "Export year must be between 2000 and 2026"
                        )
                );

        mockMvc.perform(
                        post("/api/exports/yearly")
                                .param("year", "1999")
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
                        jsonPath("$.detail")
                                .value(
                                        "Export year must be between "
                                                + "2000 and 2026"
                                )
                );
    }

    @Test
    void shouldReturnConflictForCompletedOrRunningExport()
            throws Exception {

        when(exportJobService.startAnnualExport(2022))
                .thenThrow(
                        new AnnualExportConflictException(
                                "Annual export for year 2022 "
                                        + "is already running, completed "
                                        + "or cannot currently be restarted",
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
                                .value("Annual export conflict")
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Annual export for year 2022 "
                                                + "is already running, "
                                                + "completed or cannot "
                                                + "currently be restarted"
                                )
                );
    }
}