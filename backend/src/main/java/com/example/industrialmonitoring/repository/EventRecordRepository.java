package com.example.industrialmonitoring.repository;

import com.example.industrialmonitoring.entity.EventRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRecordRepository extends JpaRepository<EventRecordEntity, Long> {

    List<EventRecordEntity> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
}