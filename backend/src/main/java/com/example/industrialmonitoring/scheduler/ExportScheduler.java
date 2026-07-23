package com.example.industrialmonitoring.scheduler;

import com.example.industrialmonitoring.config.ExportMailProperties;
import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.export.ExportPeriod;
import com.example.industrialmonitoring.service.ExportJobService;
import com.example.industrialmonitoring.service.ExportMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
    private final ExportMailService exportMailService;
    private final ExportMailProperties exportMailProperties;

    public ExportScheduler(
            ExportJobService exportJobService,
            ExportProperties exportProperties,
            ExportMailService exportMailService,
            ExportMailProperties exportMailProperties
    ) {
        this.exportJobService = exportJobService;
        this.exportProperties = exportProperties;
        this.exportMailService = exportMailService;
        this.exportMailProperties = exportMailProperties;
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

        JobExecution execution;

        try {
            execution =
                    exportJobService.startAnnualExport(exportYear);
        } catch (IllegalStateException exception) {
            LOGGER.error(
                    "Could not start scheduled annual export for year {}",
                    exportYear,
                    exception
            );

            return;
        }

        LOGGER.info(
                "Scheduled annual export finished: "
                        + "year={}, executionId={}, status={}",
                exportYear,
                execution.getId(),
                execution.getStatus()
        );

        if (execution.getStatus() != BatchStatus.COMPLETED) {
            LOGGER.warn(
                    "Skipping export email because annual export "
                            + "did not complete successfully: "
                            + "year={}, status={}",
                    exportYear,
                    execution.getStatus()
            );

            return;
        }

        sendAnnualExportByEmail(exportYear);
    }

    private void sendAnnualExportByEmail(
            int exportYear
    ) {
        if (!exportMailProperties.enabled()) {
            LOGGER.info(
                    "Scheduled export email delivery is disabled"
            );

            return;
        }

        String recipientEmail =
                exportMailProperties.annualRecipient();

        if (recipientEmail == null
                || recipientEmail.isBlank()) {

            LOGGER.error(
                    "Scheduled export email delivery is enabled, "
                            + "but no annual recipient is configured"
            );

            return;
        }

        LocalDate fromDate =
                LocalDate.of(exportYear, 1, 1);

        LocalDate toDateExclusive =
                fromDate.plusYears(1);

        ExportPeriod period =
                new ExportPeriod(
                        fromDate,
                        toDateExclusive,
                        exportProperties.zoneId()
                );

        try {
            exportMailService.sendExport(
                    period,
                    recipientEmail
            );

            LOGGER.info(
                    "Scheduled annual export email sent: "
                            + "year={}, recipient={}",
                    exportYear,
                    recipientEmail
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Annual export completed, but email delivery "
                            + "failed: year={}, recipient={}",
                    exportYear,
                    recipientEmail,
                    exception
            );
        }
    }
}