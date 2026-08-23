package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import com.example.industrialmonitoring.repository.TelemetryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TelemetryControllerIntegrationTest {

    private static final String SESSION_A = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SESSION_B = "550e8400-e29b-41d4-a716-446655440001";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("industrial_monitoring_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("mqtt.broker-url", () -> "tcp://localhost:1883");
        registry.add("mqtt.client-id", () -> "test-client-controller");
        registry.add("mqtt.topic-root", () -> "rtz");
        registry.add("mqtt.device-id", () -> "edge01");
        registry.add("mqtt.username", () -> "edge");
        registry.add("mqtt.password", () -> "edge_password");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryRecordRepository telemetryRecordRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        telemetryRecordRepository.deleteAll();

        telemetryRecordRepository.save(new TelemetryRecordEntity(
                "edge01",
                123000L,
                1L,
                BigDecimal.valueOf(30.2),
                1600
        ));
    }

    @Test
    void shouldReturnAllTelemetryRecords() throws Exception {
        mockMvc.perform(get("/api/telemetry")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value("edge01"))
                .andExpect(jsonPath("$[0].gatewayTimestamp").value(123000))
                .andExpect(jsonPath("$[0].sequenceNumber").value(1))
                .andExpect(jsonPath("$[0].temperatureC").value(30.2))
                .andExpect(jsonPath("$[0].rpm").value(1600));
    }

    @Test
    void shouldReturnLatestTelemetryRecord() throws Exception {
        mockMvc.perform(get("/api/telemetry/latest")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("edge01"))
                .andExpect(jsonPath("$.gatewayTimestamp").value(123000))
                .andExpect(jsonPath("$.sequenceNumber").value(1))
                .andExpect(jsonPath("$.temperatureC").value(30.2))
                .andExpect(jsonPath("$.rpm").value(1600));
    }

    @Test
    void shouldDistinguishLatestReceivedFromPerDeviceLatestObserved() throws Exception {
        telemetryRecordRepository.deleteAll();
        insertV3Telemetry("edge01", SESSION_B, 8, 5, "2026-08-20T10:00:00Z", "38.0");
        insertV3Telemetry("edge01", SESSION_A, 7, 200, "2026-08-20T11:00:00Z", "27.0");
        insertV3Telemetry("edge02", SESSION_A, 1, 1, "2026-08-20T12:00:00Z", "22.0");

        performGet("/api/telemetry/latest-received")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("edge02"));
        performGet("/api/telemetry/latest")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("edge02"));
        performGet("/api/telemetry/device/edge01/latest-received")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequenceNumber").value(200))
                .andExpect(jsonPath("$.sessionGeneration").value(7));
        performGet("/api/telemetry/device/edge01/latest-observed")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequenceNumber").value(5))
                .andExpect(jsonPath("$.sessionId").value(SESSION_B))
                .andExpect(jsonPath("$.sessionGeneration").value(8));
    }

    @Test
    void shouldUseHigherIdWhenLatestReceivedTimestampsTie() throws Exception {
        telemetryRecordRepository.deleteAll();
        insertV3Telemetry("edge01", SESSION_A, 7, 1, "2026-08-20T10:00:00Z", "21.0");
        insertV3Telemetry("edge02", SESSION_A, 7, 2, "2026-08-20T10:00:00Z", "22.0");

        performGet("/api/telemetry/latest-received")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("edge02"))
                .andExpect(jsonPath("$.sequenceNumber").value(2));
    }

    @Test
    void shouldNotFallBackToProtocolV2ForLatestObserved() throws Exception {
        performGet("/api/telemetry/device/edge01/latest-observed")
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnTelemetryRecordsByDeviceId() throws Exception {
        mockMvc.perform(get("/api/telemetry/device/edge01")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value("edge01"))
                .andExpect(jsonPath("$[0].temperatureC").value(30.2))
                .andExpect(jsonPath("$[0].rpm").value(1600));
    }

    @Test
    void shouldReturnPagedTelemetryRecords() throws Exception {
        mockMvc.perform(get("/api/telemetry/paged")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].deviceId").value("edge01"))
                .andExpect(jsonPath("$.content[0].temperatureC").value(30.2))
                .andExpect(jsonPath("$.content[0].rpm").value(1600))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnPagedTelemetryRecordsByDeviceId() throws Exception {
        mockMvc.perform(get("/api/telemetry/device/edge01/paged")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].deviceId").value("edge01"))
                .andExpect(jsonPath("$.content[0].temperatureC").value(30.2))
                .andExpect(jsonPath("$.content[0].rpm").value(1600))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnTelemetryRecordsByDeviceIdAndTimeRange() throws Exception {
        mockMvc.perform(get("/api/telemetry/device/edge01/range")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .param("from", "2000-01-01T00:00:00Z")
                        .param("to", "2100-01-01T00:00:00Z")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].deviceId").value("edge01"))
                .andExpect(jsonPath("$.content[0].temperatureC").value(30.2))
                .andExpect(jsonPath("$.content[0].rpm").value(1600))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test
    void shouldRejectTelemetryRequestWithoutToken() throws Exception {
        mockMvc.perform(
                        get("/api/telemetry")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidTelemetryRequestWithoutViewerRole() throws Exception {
        mockMvc.perform(
                        get("/api/telemetry")
                                .with(jwt())
                )
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions performGet(String path) throws Exception {
        return mockMvc.perform(get(path)
                .with(jwt().authorities(
                        new SimpleGrantedAuthority("ROLE_VIEWER")
                ))
                .accept(MediaType.APPLICATION_JSON));
    }

    private void insertV3Telemetry(
            String deviceId,
            String sessionId,
            long sessionGeneration,
            long sequenceNumber,
            String createdAt,
            String temperature
    ) {
        jdbc.update("""
                INSERT INTO telemetry_records (
                    device_id, session_id, session_generation,
                    gateway_timestamp, sequence_number,
                    temperature_c, rpm, created_at
                )
                VALUES (?, CAST(? AS UUID), ?, ?, ?, CAST(? AS NUMERIC), 1600, CAST(? AS TIMESTAMPTZ))
                """,
                deviceId,
                sessionId,
                sessionGeneration,
                sequenceNumber * 1000,
                sequenceNumber,
                temperature,
                createdAt
        );
    }
}
