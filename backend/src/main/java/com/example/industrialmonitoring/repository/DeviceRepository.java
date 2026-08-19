package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    Optional<DeviceEntity> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);

    @Modifying
    @Query(value = """
            INSERT INTO devices (device_id)
            VALUES (:deviceId)
            ON CONFLICT (device_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("deviceId") String deviceId);
}
