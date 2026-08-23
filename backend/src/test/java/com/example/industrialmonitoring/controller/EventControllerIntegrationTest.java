package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.entity.EventRecordEntity;
import com.example.industrialmonitoring.repository.EventRecordRepository;
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
class EventControllerIntegrationTest {

    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

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
        registry.add("mqtt.client-id", () -> "test-client-event-controller");
        registry.add("mqtt.topic-root", () -> "rtz");
        registry.add("mqtt.device-id", () -> "edge01");
        registry.add("mqtt.username", () -> "edge");
        registry.add("mqtt.password", () -> "edge_password");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRecordRepository eventRecordRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        eventRecordRepository.deleteAll();

        eventRecordRepository.save(new EventRecordEntity(
                "edge01",
                123001L,
                43L,
                "ALARM_RAISED"
        ));
    }

    @Test
    void shouldReturnAllEvents() throws Exception {
        mockMvc.perform(get("/api/events")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value("edge01"))
                .andExpect(jsonPath("$[0].gatewayTimestamp").value(123001))
                .andExpect(jsonPath("$[0].sequenceNumber").value(43))
                .andExpect(jsonPath("$[0].eventType").value("ALARM_RAISED"));
    }

    @Test
    void shouldReturnGlobalEventsByCreatedAtThenIdDescending() throws Exception {
        eventRecordRepository.deleteAll();
        insertV3Event(1, "FIRST", "2026-08-20T10:00:00Z");
        insertV3Event(2, "SECOND", "2026-08-20T10:00:00Z");

        mockMvc.perform(get("/api/events")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sequenceNumber").value(2))
                .andExpect(jsonPath("$[0].eventType").value("SECOND"))
                .andExpect(jsonPath("$[0].sessionId").value(SESSION_ID))
                .andExpect(jsonPath("$[0].sessionGeneration").value(7))
                .andExpect(jsonPath("$[1].sequenceNumber").value(1));
    }

    @Test
    void shouldReturnEventsByDeviceId() throws Exception {
        mockMvc.perform(get("/api/events/device/edge01")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_VIEWER")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value("edge01"))
                .andExpect(jsonPath("$[0].eventType").value("ALARM_RAISED"));
    }

    private void insertV3Event(long sequenceNumber, String eventType, String createdAt) {
        jdbc.update("""
                INSERT INTO event_records (
                    device_id, session_id, session_generation,
                    gateway_timestamp, sequence_number, event_type, created_at
                )
                VALUES (
                    'edge01', CAST(? AS UUID), 7,
                    ?, ?, ?, CAST(? AS TIMESTAMPTZ)
                )
                """,
                SESSION_ID,
                sequenceNumber * 1000,
                sequenceNumber,
                eventType,
                createdAt
        );
    }
}
