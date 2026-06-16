package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.entity.HealthRecordEntity;
import com.example.industrialmonitoring.repository.HealthRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class HealthControllerIntegrationTest {

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
    void shouldReturnHealthRecordsByDeviceId() throws Exception {
        mockMvc.perform(get("/api/health/device/edge01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value("edge01"))
                .andExpect(jsonPath("$[0].state").value(2))
                .andExpect(jsonPath("$[0].mqttConnected").value(true))
                .andExpect(jsonPath("$[0].pubLastOk").value(true));
    }
}