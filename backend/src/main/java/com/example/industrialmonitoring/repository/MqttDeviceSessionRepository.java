package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.MqttDeviceSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MqttDeviceSessionRepository
        extends JpaRepository<MqttDeviceSessionEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO mqtt_device_sessions (
                device_id, session_generation, session_id
            )
            VALUES (:deviceId, :sessionGeneration, :sessionId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("deviceId") String deviceId,
            @Param("sessionGeneration") Long sessionGeneration,
            @Param("sessionId") UUID sessionId
    );

    boolean existsByDeviceIdAndSessionGenerationAndSessionId(
            String deviceId,
            Long sessionGeneration,
            UUID sessionId
    );
}
