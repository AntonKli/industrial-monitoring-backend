package com.example.industrialmonitoring.controller;

import com.example.industrialmonitoring.dto.EventRecordResponse;
import com.example.industrialmonitoring.service.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventRecordResponse> getAllEvents() {
        return eventService.findAllEvents();
    }

    @GetMapping("/device/{deviceId}")
    public List<EventRecordResponse> getEventsByDeviceId(@PathVariable String deviceId) {
        return eventService.findEventsByDeviceId(deviceId);
    }
}