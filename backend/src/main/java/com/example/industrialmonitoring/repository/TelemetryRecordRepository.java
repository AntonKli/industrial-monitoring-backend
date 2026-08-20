package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelemetryRecordRepository extends JpaRepository<TelemetryRecordEntity, Long> {

    List<TelemetryRecordEntity> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    Page<TelemetryRecordEntity> findByDeviceIdOrderByCreatedAtDesc(
            String deviceId,
            Pageable pageable
    );

    Page<TelemetryRecordEntity> findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String deviceId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    Optional<TelemetryRecordEntity> findFirstByOrderByCreatedAtDesc();

    Optional<TelemetryRecordEntity> findFirstByDeviceIdOrderByCreatedAtDesc(String deviceId);

    @Modifying
    @Query(value = """
            INSERT INTO telemetry_records (
                device_id, session_id, gateway_timestamp, sequence_number,
                temperature_c, rpm
            )
            VALUES (
                :deviceId, :sessionId, :gatewayTimestamp, :sequenceNumber,
                :temperatureC, :rpm
            )
            ON CONFLICT (device_id, session_id, sequence_number) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("deviceId") String deviceId,
            @Param("sessionId") UUID sessionId,
            @Param("gatewayTimestamp") Long gatewayTimestamp,
            @Param("sequenceNumber") Long sequenceNumber,
            @Param("temperatureC") BigDecimal temperatureC,
            @Param("rpm") Integer rpm
    );
}
