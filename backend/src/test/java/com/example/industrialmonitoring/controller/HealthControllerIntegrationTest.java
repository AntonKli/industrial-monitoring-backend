package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.entity.HealthRecordEntity;
import com.example.industrialmonitoring.repository.HealthRecordRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class HealthControllerIntegrationTest {

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

        registry.add("mqtt.subscriber.enabled", () -> false);
        registry.add("mqtt.broker-url", () -> "tcp://localhost:1883");
        registry.add("mqtt.client-id", () -> "test-client-health-controller");
        registry.add("mqtt.topic-root", () -> "rtz");
        registry.add("mqtt.device-id", () -> "edge01");
        registry.add("mqtt.username", () -> "edge");
        registry.add("mqtt.password", () -> "edge_password");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        healthRecordRepository.deleteAll();

        healthRecordRepository.save(new HealthRecordEntity(
                "edge01",
                123002L,
                44L,
                2,
                true,
                true,
                0,
                0L,
                123L,
                1L,
                120L,
                0L,
                0
        ));
    }

    @Test
    void shouldReturnAllHealthRecords() throws Exception {
        mockMvc.perform(get("/api/health")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value("edge01"))
                .andExpect(jsonPath("$[0].gatewayTimestamp").value(123002))
                .andExpect(jsonPath("$[0].sequenceNumber").value(44))
                .andExpect(jsonPath("$[0].state").value(2))
                .andExpect(jsonPath("$[0].mqttConnected").value(true))
                .andExpect(jsonPath("$[0].pubLastOk").value(true))
                .andExpect(jsonPath("$[0].bufferFill").value(0))
                .andExpect(jsonPath("$[0].bufferDrops").value(0))
                .andExpect(jsonPath("$[0].diagUptimeS").value(123))
                .andExpect(jsonPath("$[0].diagReconnects").value(1))
                .andExpect(jsonPath("$[0].diagPubOk").value(120))
                .andExpect(jsonPath("$[0].diagPubFail").value(0))
                .andExpect(jsonPath("$[0].diagLastError").value(0));
    }

    @Test
    void shouldReturnLatestHealthRecord() throws Exception {
        mockMvc.perform(get("/api/health/latest")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("edge01"))
                .andExpect(jsonPath("$.gatewayTimestamp").value(123002))
                .andExpect(jsonPath("$.sequenceNumber").value(44))
                .andExpect(jsonPath("$.state").value(2))
                .andExpect(jsonPath("$.mqttConnected").value(true))
                .andExpect(jsonPath("$.pubLastOk").value(true));
    }

    @Test
    void shouldDistinguishLatestReceivedFromPerDeviceLatestObserved() throws Exception {
        healthRecordRepository.deleteAll();
        insertV3Health("edge01", SESSION_B, 8, 5, "2026-08-20T10:00:00Z", 8);
        insertV3Health("edge01", SESSION_A, 7, 200, "2026-08-20T11:00:00Z", 7);
        insertV3Health("edge02", SESSION_A, 1, 1, "2026-08-20T12:00:00Z", 1);

        performGet("/api/health/latest-received")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("edge02"));
        performGet("/api/health/latest")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("edge02"));
        performGet("/api/health/device/edge01/latest-received")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequenceNumber").value(200))
                .andExpect(jsonPath("$.sessionGeneration").value(7));
        performGet("/api/health/device/edge01/latest-observed")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequenceNumber").value(5))
                .andExpect(jsonPath("$.sessionId").value(SESSION_B))
                .andExpect(jsonPath("$.sessionGeneration").value(8))
                .andExpect(jsonPath("$.state").value(8));
    }

    @Test
    void shouldUseHigherIdWhenLatestReceivedTimestampsTie() throws Exception {
        healthRecordRepository.deleteAll();
        insertV3Health("edge01", SESSION_A, 7, 1, "2026-08-20T10:00:00Z", 1);
        insertV3Health("edge02", SESSION_A, 7, 2, "2026-08-20T10:00:00Z", 2);

        performGet("/api/health/latest-received")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("edge02"))
                .andExpect(jsonPath("$.sequenceNumber").value(2));
    }

    @Test
    void shouldNotFallBackToProtocolV2ForLatestObserved() throws Exception {
        performGet("/api/health/device/edge01/latest-observed")
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnHealthRecordsByDeviceId() throws Exception {
        mockMvc.perform(get("/api/health/device/edge01")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value("edge01"))
                .andExpect(jsonPath("$[0].state").value(2))
                .andExpect(jsonPath("$[0].mqttConnected").value(true))
                .andExpect(jsonPath("$[0].pubLastOk").value(true));
    }

    private org.springframework.test.web.servlet.ResultActions performGet(String path) throws Exception {
        return mockMvc.perform(get(path)
                .with(jwt().authorities(
                        new SimpleGrantedAuthority("ROLE_VIEWER")
                ))
                .accept(MediaType.APPLICATION_JSON));
    }

    private void insertV3Health(
            String deviceId,
            String sessionId,
            long sessionGeneration,
            long sequenceNumber,
            String createdAt,
            int state
    ) {
        jdbc.update("""
                INSERT INTO health_records (
                    device_id, session_id, session_generation,
                    gateway_timestamp, sequence_number, state, created_at
                )
                VALUES (?, CAST(? AS UUID), ?, ?, ?, ?, CAST(? AS TIMESTAMPTZ))
                """,
                deviceId,
                sessionId,
                sessionGeneration,
                sequenceNumber * 1000,
                sequenceNumber,
                state,
                createdAt
        );
    }
}
