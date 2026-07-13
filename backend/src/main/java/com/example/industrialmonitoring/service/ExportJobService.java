package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.batch.AnnualExportJobConfig;
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

    public ExportJobService(
            JobLauncher jobLauncher,
            @Qualifier(AnnualExportJobConfig.JOB_NAME)
            Job annualMonitoringExportJob
    ) {
        this.jobLauncher = jobLauncher;
        this.annualMonitoringExportJob = annualMonitoringExportJob;
    }

    public JobExecution startAnnualExport(int year) {
        validateYear(year);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("year", (long) year)
                .toJobParameters();

        try {
            return jobLauncher.run(
                    annualMonitoringExportJob,
                    jobParameters
            );
        } catch (
                JobExecutionAlreadyRunningException
                | JobRestartException
                | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException exception
        ) {
            throw new IllegalStateException(
                    "Could not start annual export for year " + year,
                    exception
            );
        }
    }

    private void validateYear(int year) {
        int currentYear = Year.now().getValue();

        if (year < 2000 || year > currentYear) {
            throw new IllegalArgumentException(
                    "Export year must be between 2000 and "
                            + currentYear
            );
        }
    }
}