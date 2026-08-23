package com.example.industrialmonitoring.dto;

public interface VersionedMqttMessage {

    Integer v();

    Long sessionGeneration();
}
