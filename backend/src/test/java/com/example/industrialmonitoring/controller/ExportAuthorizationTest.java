package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.export.ExportPeriod;
import com.example.industrialmonitoring.service.ExportMailService;
import com.example.industrialmonitoring.service.ExportPreparationService;
import com.example.industrialmonitoring.config.SecurityConfig;
import com.example.industrialmonitoring.service.ExportFileService;
import com.example.industrialmonitoring.service.ExportJobService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ExportController.class)
@Import(SecurityConfig.class)
class ExportAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportJobService exportJobService;

    @MockitoBean
    private ExportFileService exportFileService;

    @MockitoBean
    private ExportPreparationService exportPreparationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ExportMailService exportMailService;

    @Test
    void shouldRejectExportRequestWithoutToken() throws Exception {
        mockMvc.perform(
                        post("/api/exports/yearly")
                                .param("year", "2022")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidExportRequestWithoutOperatorRole() throws Exception {
        mockMvc.perform(
                        post("/api/exports/yearly")
                                .param("year", "2022")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_VIEWER"
                                        )
                                ))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidEmailExportForViewerRole()
            throws Exception {

        String requestBody =
                "{"
                        + "\"fromDate\":\"2025-01-01\","
                        + "\"toDateExclusive\":\"2026-01-01\","
                        + "\"recipientEmail\":\"viewer@example.com\""
                        + "}";

        mockMvc.perform(
                        post("/api/exports/range/email")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_VIEWER"
                                                )
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                exportPreparationService,
                exportMailService
        );
    }

    @Test
    void shouldAllowEmailExportForOperatorRole()
            throws Exception {

        LocalDate fromDate =
                LocalDate.of(2025, 1, 1);

        LocalDate toDateExclusive =
                LocalDate.of(2026, 1, 1);

        ExportPeriod period =
                new ExportPeriod(
                        fromDate,
                        toDateExclusive,
                        ZoneId.of("Europe/Berlin")
                );

        when(
                exportPreparationService.prepareRangeExport(
                        fromDate,
                        toDateExclusive
                )
        ).thenReturn(period);

        String requestBody =
                "{"
                        + "\"fromDate\":\"2025-01-01\","
                        + "\"toDateExclusive\":\"2026-01-01\","
                        + "\"recipientEmail\":\"operator@example.com\""
                        + "}";

        mockMvc.perform(
                        post("/api/exports/range/email")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_OPERATOR"
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.recipientEmail")
                                .value("operator@example.com")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("SENT")
                );

        verify(exportPreparationService)
                .prepareRangeExport(
                        fromDate,
                        toDateExclusive
                );

        verify(exportMailService)
                .sendExport(
                        period,
                        "operator@example.com"
                );
    }

    @Test
    void shouldAllowExportRequestWithOperatorRole() throws Exception {
        JobExecution jobExecution = mock(JobExecution.class);
        JobInstance jobInstance = mock(JobInstance.class);

        when(exportJobService.startAnnualExport(2022))
                .thenReturn(jobExecution);
        when(jobExecution.getId())
                .thenReturn(42L);
        when(jobExecution.getJobInstance())
                .thenReturn(jobInstance);
        when(jobInstance.getJobName())
                .thenReturn("annualMonitoringExportJob");
        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.COMPLETED);

        mockMvc.perform(
                        post("/api/exports/yearly")
                                .param("year", "2022")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_OPERATOR"
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(42))
                .andExpect(
                        jsonPath("$.jobName")
                                .value("annualMonitoringExportJob")
                )
                .andExpect(jsonPath("$.year").value(2022))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}