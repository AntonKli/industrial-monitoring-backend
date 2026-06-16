package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TelemetryRecordRepository extends JpaRepository<TelemetryRecordEntity, Long> {

    List<TelemetryRecordEntity> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    Page<TelemetryRecordEntity> findByDeviceIdOrderByCreatedAtDesc(String deviceId, Pageable pageable);

    Optional<TelemetryRecordEntity> findFirstByOrderByCreatedAtDesc();

    Optional<TelemetryRecordEntity> findFirstByDeviceIdOrderByCreatedAtDesc(String deviceId);
}