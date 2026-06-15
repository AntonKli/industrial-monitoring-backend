package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.HealthRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HealthRecordRepository extends JpaRepository<HealthRecordEntity, Long> {

    List<HealthRecordEntity> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    Optional<HealthRecordEntity> findFirstByOrderByCreatedAtDesc();

    Optional<HealthRecordEntity> findFirstByDeviceIdOrderByCreatedAtDesc(String deviceId);
}