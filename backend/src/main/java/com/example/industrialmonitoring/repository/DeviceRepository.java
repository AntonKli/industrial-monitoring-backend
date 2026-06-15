package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    Optional<DeviceEntity> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);
}