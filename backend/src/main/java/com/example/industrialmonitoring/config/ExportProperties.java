package com.example.industrialmonitoring.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.time.ZoneId;

@Validated
@ConfigurationProperties(prefix = "export")
public record ExportProperties(
        boolean enabled,
@NotBlank String outputDir,
@Min(1) int chunkSize,
@NotBlank String cron,
@NotBlank String zone
        ) {

public Path outputPath() {
        return Path.of(outputDir)
        .toAbsolutePath()
        .normalize();
        }

public ZoneId zoneId() {
        return ZoneId.of(zone);
        }
        }