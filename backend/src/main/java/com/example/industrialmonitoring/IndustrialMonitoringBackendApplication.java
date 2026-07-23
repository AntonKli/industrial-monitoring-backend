package com.example.industrialmonitoring;

import com.example.industrialmonitoring.config.ExportMailProperties;
import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.config.MqttProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        MqttProperties.class,
        ExportProperties.class,
        ExportMailProperties.class
})
public class IndustrialMonitoringBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                IndustrialMonitoringBackendApplication.class,
                args
        );
    }
}