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

    List<TelemetryRecordEntity> findAllByOrderByCreatedAtDescIdDesc();

    Page<TelemetryRecordEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    List<TelemetryRecordEntity> findByDeviceIdOrderByCreatedAtDescIdDesc(String deviceId);

    Page<TelemetryRecordEntity> findByDeviceIdOrderByCreatedAtDescIdDesc(
            String deviceId,
            Pageable pageable
    );

    Page<TelemetryRecordEntity> findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDescIdDesc(
            String deviceId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    Optional<TelemetryRecordEntity> findFirstByOrderByCreatedAtDescIdDesc();

    Optional<TelemetryRecordEntity> findFirstByDeviceIdOrderByCreatedAtDescIdDesc(String deviceId);

    @Query(value = """
            SELECT *
            FROM telemetry_records
            WHERE device_id = :deviceId
              AND session_generation IS NOT NULL
              AND session_id IS NOT NULL
            ORDER BY session_generation DESC,
                     sequence_number DESC,
                     gateway_timestamp DESC,
                     created_at DESC,
                     id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<TelemetryRecordEntity> findLatestObservedByDeviceId(
            @Param("deviceId") String deviceId
    );

    @Modifying
    @Query(value = """
            INSERT INTO telemetry_records (
                device_id, session_id, session_generation,
                gateway_timestamp, sequence_number, temperature_c, rpm
            )
            VALUES (
                :deviceId, :sessionId, :sessionGeneration,
                :gatewayTimestamp, :sequenceNumber, :temperatureC, :rpm
            )
            ON CONFLICT (device_id, session_id, sequence_number) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("deviceId") String deviceId,
            @Param("sessionId") UUID sessionId,
            @Param("sessionGeneration") Long sessionGeneration,
            @Param("gatewayTimestamp") Long gatewayTimestamp,
            @Param("sequenceNumber") Long sequenceNumber,
            @Param("temperatureC") BigDecimal temperatureC,
            @Param("rpm") Integer rpm
    );
}
