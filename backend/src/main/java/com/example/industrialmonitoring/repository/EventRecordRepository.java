package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.EventRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventRecordRepository extends JpaRepository<EventRecordEntity, Long> {

    List<EventRecordEntity> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    @Modifying
    @Query(value = """
            INSERT INTO event_records (
                device_id, session_id, gateway_timestamp, sequence_number,
                event_type
            )
            VALUES (
                :deviceId, :sessionId, :gatewayTimestamp, :sequenceNumber,
                :eventType
            )
            ON CONFLICT (device_id, session_id, sequence_number) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("deviceId") String deviceId,
            @Param("sessionId") UUID sessionId,
            @Param("gatewayTimestamp") Long gatewayTimestamp,
            @Param("sequenceNumber") Long sequenceNumber,
            @Param("eventType") String eventType
    );
}
