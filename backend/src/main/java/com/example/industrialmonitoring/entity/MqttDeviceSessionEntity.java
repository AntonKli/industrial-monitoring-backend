package com.example.industrialmonitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mqtt_device_sessions")
public class MqttDeviceSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "session_generation", nullable = false)
    private Long sessionGeneration;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "first_received_at", nullable = false)
    private OffsetDateTime firstReceivedAt;

    protected MqttDeviceSessionEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Long getSessionGeneration() {
        return sessionGeneration;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public OffsetDateTime getFirstReceivedAt() {
        return firstReceivedAt;
    }
}
