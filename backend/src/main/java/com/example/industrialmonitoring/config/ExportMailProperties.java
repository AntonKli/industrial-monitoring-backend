package com.example.industrialmonitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "export.mail")
public record ExportMailProperties(
        boolean enabled,
        String from,
        String annualRecipient
) {
}