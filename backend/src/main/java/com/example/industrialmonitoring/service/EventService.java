package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.dto.EventRecordResponse;
import com.example.industrialmonitoring.mapper.EventRecordMapper;
import com.example.industrialmonitoring.repository.EventRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {

    private final EventRecordRepository eventRecordRepository;
    private final EventRecordMapper eventRecordMapper;

    public EventService(
            EventRecordRepository eventRecordRepository,
            EventRecordMapper eventRecordMapper
    ) {
        this.eventRecordRepository = eventRecordRepository;
        this.eventRecordMapper = eventRecordMapper;
    }

    @Transactional(readOnly = true)
    public List<EventRecordResponse> findAllEvents() {
        return eventRecordRepository.findAll()
                .stream()
                .map(eventRecordMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventRecordResponse> findEventsByDeviceId(String deviceId) {
        return eventRecordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId)
                .stream()
                .map(eventRecordMapper::toResponse)
                .toList();
    }
}