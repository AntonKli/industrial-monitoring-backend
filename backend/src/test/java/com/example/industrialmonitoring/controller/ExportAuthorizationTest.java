package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.config.ExportProperties;
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
    private ExportProperties exportProperties;

    @MockitoBean
    private JwtDecoder jwtDecoder;

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