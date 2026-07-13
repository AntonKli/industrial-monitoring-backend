package com.example.industrialmonitoring.scheduler;

import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.service.ExportJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
@ConditionalOnProperty(
        prefix = "export",
        name = "enabled",
        havingValue = "true"
)
public class ExportScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ExportScheduler.class);

    private final ExportJobService exportJobService;
    private final ExportProperties exportProperties;

    public ExportScheduler(
            ExportJobService exportJobService,
            ExportProperties exportProperties
    ) {
        this.exportJobService = exportJobService;
        this.exportProperties = exportProperties;
    }

    @Scheduled(
            cron = "${export.cron}",
            zone = "${export.zone}"
    )
    public void exportPreviousYear() {
        int exportYear = Year.now(exportProperties.zoneId())
                .minusYears(1)
                .getValue();

        LOGGER.info(
                "Starting scheduled annual export for year {}",
                exportYear
        );

        try {
            JobExecution execution =
                    exportJobService.startAnnualExport(exportYear);

            LOGGER.info(
                    "Scheduled annual export submitted: "
                            + "year={}, executionId={}, status={}",
                    exportYear,
                    execution.getId(),
                    execution.getStatus()
            );
        } catch (IllegalStateException exception) {
            LOGGER.error(
                    "Could not start scheduled annual export for year {}",
                    exportYear,
                    exception
            );
        }
    }
}