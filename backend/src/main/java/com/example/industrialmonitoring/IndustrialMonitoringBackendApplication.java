package com.example.industrialmonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.example.industrialmonitoring.config.MqttProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(MqttProperties.class)
@SpringBootApplication
public class IndustrialMonitoringBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(IndustrialMonitoringBackendApplication.class, args);
	}

}
