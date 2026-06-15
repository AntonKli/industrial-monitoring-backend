package com.example.industrialmonitoring.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "health_records")
public class HealthRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "gateway_timestamp", nullable = false)
    private Long gatewayTimestamp;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(name = "state")
    private Integer state;

    @Column(name = "mqtt_connected")
    private Boolean mqttConnected;

    @Column(name = "pub_last_ok")
    private Boolean pubLastOk;

    @Column(name = "buffer_fill")
    private Integer bufferFill;

    @Column(name = "buffer_drops")
    private Long bufferDrops;

    @Column(name = "diag_uptime_s")
    private Long diagUptimeS;

    @Column(name = "diag_reconnects")
    private Long diagReconnects;

    @Column(name = "diag_pub_ok")
    private Long diagPubOk;

    @Column(name = "diag_pub_fail")
    private Long diagPubFail;

    @Column(name = "diag_last_error")
    private Integer diagLastError;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected HealthRecordEntity() {
    }

    public HealthRecordEntity(
            String deviceId,
            Long gatewayTimestamp,
            Long sequenceNumber,
            Integer state,
            Boolean mqttConnected,
            Boolean pubLastOk,
            Integer bufferFill,
            Long bufferDrops,
            Long diagUptimeS,
            Long diagReconnects,
            Long diagPubOk,
            Long diagPubFail,
            Integer diagLastError
    ) {
        this.deviceId = deviceId;
        this.gatewayTimestamp = gatewayTimestamp;
        this.sequenceNumber = sequenceNumber;
        this.state = state;
        this.mqttConnected = mqttConnected;
        this.pubLastOk = pubLastOk;
        this.bufferFill = bufferFill;
        this.bufferDrops = bufferDrops;
        this.diagUptimeS = diagUptimeS;
        this.diagReconnects = diagReconnects;
        this.diagPubOk = diagPubOk;
        this.diagPubFail = diagPubFail;
        this.diagLastError = diagLastError;
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

    public Integer getState() {
        return state;
    }

    public Boolean getMqttConnected() {
        return mqttConnected;
    }

    public Boolean getPubLastOk() {
        return pubLastOk;
    }

    public Integer getBufferFill() {
        return bufferFill;
    }

    public Long getBufferDrops() {
        return bufferDrops;
    }

    public Long getDiagUptimeS() {
        return diagUptimeS;
    }

    public Long getDiagReconnects() {
        return diagReconnects;
    }

    public Long getDiagPubOk() {
        return diagPubOk;
    }

    public Long getDiagPubFail() {
        return diagPubFail;
    }

    public Integer getDiagLastError() {
        return diagLastError;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}