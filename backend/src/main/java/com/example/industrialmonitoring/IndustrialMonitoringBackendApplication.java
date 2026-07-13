package com.example.industrialmonitoring;

import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.config.MqttProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({
        MqttProperties.class,
        ExportProperties.class
})
@SpringBootApplication
public class IndustrialMonitoringBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                IndustrialMonitoringBackendApplication.class,
                args
        );
    }
}