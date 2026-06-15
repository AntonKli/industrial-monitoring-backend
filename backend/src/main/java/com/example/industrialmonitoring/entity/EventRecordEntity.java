package com.example.industrialmonitoring.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "event_records")
public class EventRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "gateway_timestamp", nullable = false)
    private Long gatewayTimestamp;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected EventRecordEntity() {
    }

    public EventRecordEntity(String deviceId, Long gatewayTimestamp, Long sequenceNumber, String eventType) {
        this.deviceId = deviceId;
        this.gatewayTimestamp = gatewayTimestamp;
        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
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

    public String getEventType() {
        return eventType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}