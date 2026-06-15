package com.example.industrialmonitoring.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "telemetry_records")
public class TelemetryRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "gateway_timestamp", nullable = false)
    private Long gatewayTimestamp;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(name = "temperature_c", precision = 6, scale = 2)
    private BigDecimal temperatureC;

    @Column(name = "rpm")
    private Integer rpm;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TelemetryRecordEntity() {
    }

    public TelemetryRecordEntity(String deviceId, Long gatewayTimestamp, Long sequenceNumber, BigDecimal temperatureC, Integer rpm) {
        this.deviceId = deviceId;
        this.gatewayTimestamp = gatewayTimestamp;
        this.sequenceNumber = sequenceNumber;
        this.temperatureC = temperatureC;
        this.rpm = rpm;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Long getGatewayTimestamp() {
        return gatewayTimestamp;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public BigDecimal getTemperatureC() {
        return temperatureC;
    }

    public Integer getRpm() {
        return rpm;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}