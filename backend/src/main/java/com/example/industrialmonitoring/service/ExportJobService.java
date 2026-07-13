package com.example.industrialmonitoring.service;

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

@Service
public class ExportJobService {

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
        validateYear(year);

        ExportPeriod period = ExportPeriod.forYear(
                year,
                exportProperties.zoneId()
        );

        JobParameters jobParameters =
                createTransitionalJobParameters(year, period);

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
                    "Annual export for year "
                            + year
                            + " is already running, completed "
                            + "or cannot currently be restarted",
                    exception
            );
        } catch (JobParametersInvalidException exception) {
            throw new IllegalStateException(
                    "Annual export job parameters are invalid "
                            + "for year "
                            + year,
                    exception
            );
        }
    }

    private JobParameters createTransitionalJobParameters(
            int year,
            ExportPeriod period
    ) {
        return new JobParametersBuilder()
                /*
                 * The existing batch configuration still uses year.
                 * It remains the identifying parameter during this
                 * transitional implementation step.
                 */
                .addLong("year", (long) year)

                /*
                 * These values prepare the migration to flexible
                 * ranges but are temporarily non-identifying.
                 */
                .addString(
                        "fromDate",
                        period.fromDate().toString(),
                        false
                )
                .addString(
                        "toDateExclusive",
                        period.toDateExclusive().toString(),
                        false
                )
                .addString(
                        "zoneId",
                        period.zoneId().getId(),
                        false
                )
                .toJobParameters();
    }

    private void validateYear(int year) {
        int currentYear = Year.now().getValue();

        if (year < 2000 || year > currentYear) {
            throw new InvalidExportYearException(
                    "Export year must be between 2000 and "
                            + currentYear
            );
        }
    }
}