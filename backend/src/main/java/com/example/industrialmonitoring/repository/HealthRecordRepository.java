package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.HealthRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecordEntity, Long> {

    List<HealthRecordEntity> findAllByOrderByCreatedAtDescIdDesc();

    List<HealthRecordEntity> findByDeviceIdOrderByCreatedAtDescIdDesc(String deviceId);

    Optional<HealthRecordEntity> findFirstByOrderByCreatedAtDescIdDesc();

    Optional<HealthRecordEntity> findFirstByDeviceIdOrderByCreatedAtDescIdDesc(String deviceId);

    @Query(value = """
            SELECT *
            FROM health_records
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
    Optional<HealthRecordEntity> findLatestObservedByDeviceId(
            @Param("deviceId") String deviceId
    );

    @Modifying
    @Query(value = """
            INSERT INTO health_records (
                device_id, session_id, session_generation,
                gateway_timestamp, sequence_number, state,
                mqtt_connected, pub_last_ok, buffer_fill, buffer_drops,
                diag_uptime_s, diag_reconnects, diag_pub_ok, diag_pub_fail,
                diag_last_error
            )
            VALUES (
                :deviceId, :sessionId, :sessionGeneration,
                :gatewayTimestamp, :sequenceNumber, :state,
                :mqttConnected, :pubLastOk, :bufferFill, :bufferDrops,
                :diagUptimeS, :diagReconnects, :diagPubOk, :diagPubFail,
                :diagLastError
            )
            ON CONFLICT (device_id, session_id, sequence_number) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("deviceId") String deviceId,
            @Param("sessionId") UUID sessionId,
            @Param("sessionGeneration") Long sessionGeneration,
            @Param("gatewayTimestamp") Long gatewayTimestamp,
            @Param("sequenceNumber") Long sequenceNumber,
            @Param("state") Integer state,
            @Param("mqttConnected") Boolean mqttConnected,
            @Param("pubLastOk") Boolean pubLastOk,
            @Param("bufferFill") Integer bufferFill,
            @Param("bufferDrops") Long bufferDrops,
            @Param("diagUptimeS") Long diagUptimeS,
            @Param("diagReconnects") Long diagReconnects,
            @Param("diagPubOk") Long diagPubOk,
            @Param("diagPubFail") Long diagPubFail,
            @Param("diagLastError") Integer diagLastError
    );
}
