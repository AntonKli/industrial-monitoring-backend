package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.exception.InvalidExportPeriodException;
import com.example.industrialmonitoring.batch.AnnualExportJobConfig;
import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.exception.AnnualExportConflictException;
import com.example.industrialmonitoring.exception.InvalidExportYearException;
import com.example.industrialmonitoring.export.ExportPeriod;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.time.LocalDate;

@Service
public class ExportJobService {

    private static final int MINIMUM_EXPORT_YEAR = 2000;
    private static final LocalDate MINIMUM_EXPORT_DATE =
        LocalDate.of(MINIMUM_EXPORT_YEAR, 1, 1);

    private final JobLauncher jobLauncher;
    private final Job annualMonitoringExportJob;
    private final ExportProperties exportProperties;

    public ExportJobService(
            JobLauncher jobLauncher,
            @Qualifier(AnnualExportJobConfig.JOB_NAME)
            Job annualMonitoringExportJob,
            ExportProperties exportProperties
    ) {
        this.jobLauncher = jobLauncher;
        this.annualMonitoringExportJob =
                annualMonitoringExportJob;
        this.exportProperties = exportProperties;
    }

    public JobExecution startAnnualExport(int year) {
        validateCompletedYear(year);

        ExportPeriod period = ExportPeriod.forYear(
                year,
                exportProperties.zoneId()
        );

        return startExport(period);
    }

    public JobExecution startRangeExport(
        LocalDate fromDate,
        LocalDate toDateExclusive
) {
    validateRange(fromDate, toDateExclusive);

    ExportPeriod period = new ExportPeriod(
            fromDate,
            toDateExclusive,
            exportProperties.zoneId()
    );

    return startExport(period);
}
    private JobExecution startExport(ExportPeriod period) {
        JobParameters jobParameters =
                createJobParameters(period);

        try {
            return jobLauncher.run(
                    annualMonitoringExportJob,
                    jobParameters
            );
        } catch (
                JobExecutionAlreadyRunningException
                | JobRestartException
                | JobInstanceAlreadyCompleteException exception
        ) {
            throw new AnnualExportConflictException(
                    "Export for period "
                            + period.exportKey()
                            + " is already running, completed "
                            + "or cannot currently be restarted",
                    exception
            );
        } catch (JobParametersInvalidException exception) {
            throw new IllegalStateException(
                    "Export job parameters are invalid for period "
                            + period.exportKey(),
                    exception
            );
        }
    }

    private JobParameters createJobParameters(
            ExportPeriod period
    ) {
        return new JobParametersBuilder()
                .addString(
                        "fromDate",
                        period.fromDate().toString(),
                        true
                )
                .addString(
                        "toDateExclusive",
                        period.toDateExclusive().toString(),
                        true
                )
                .addString(
                        "zoneId",
                        period.zoneId().getId(),
                        true
                )
                .toJobParameters();
    }

    private void validateRange(
        LocalDate fromDate,
        LocalDate toDateExclusive
) {
    if (fromDate == null || toDateExclusive == null) {
        throw new InvalidExportPeriodException(
                "Export period must contain a start "
                        + "and an exclusive end date"
        );
    }

    if (!fromDate.isBefore(toDateExclusive)) {
        throw new InvalidExportPeriodException(
                "Export start date must be before "
                        + "the exclusive end date"
        );
    }

    if (fromDate.isBefore(MINIMUM_EXPORT_DATE)) {
        throw new InvalidExportPeriodException(
                "Export start date must not be before "
                        + MINIMUM_EXPORT_DATE
        );
    }

    LocalDate latestAllowedEnd =
            LocalDate.now(exportProperties.zoneId())
                    .plusDays(1);

    if (toDateExclusive.isAfter(latestAllowedEnd)) {
        throw new InvalidExportPeriodException(
                "Export end date must not be after "
                        + latestAllowedEnd
        );
    }
}
    private void validateCompletedYear(int year) {
        int latestCompletedYear = Year.now(
                exportProperties.zoneId()
        ).getValue() - 1;

        if (
                year < MINIMUM_EXPORT_YEAR
                        || year > latestCompletedYear
        ) {
            throw new InvalidExportYearException(
                    "Export year must be between "
                            + MINIMUM_EXPORT_YEAR
                            + " and "
                            + latestCompletedYear
                            + ". Use a range export for "
                            + "the current year."
            );
        }
    }
}