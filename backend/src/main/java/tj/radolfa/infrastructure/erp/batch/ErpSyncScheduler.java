package tj.radolfa.infrastructure.erp.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly reconciliation trigger.
 *
 * Programmatically launches {@code erpInitialImportJob} so that any
 * products added or updated in ERPNext since the last run are pulled
 * into Radolfa.
 *
 * A unique {@code run.id} parameter is injected into every execution
 * so that Spring Batch does not skip the job as a duplicate.
 *
 * Active only on {@code dev} and {@code test} profiles to prevent
 * accidental hourly hammering during CI or production setup.
 * Remove / adjust the {@code @Profile} when the production schedule
 * is confirmed.
 */
@Component
@Profile({"dev", "test"})
public class ErpSyncScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ErpSyncScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job         erpInitialImportJob;

    public ErpSyncScheduler(JobLauncher jobLauncher, Job erpInitialImportJob) {
        this.jobLauncher        = jobLauncher;
        this.erpInitialImportJob = erpInitialImportJob;
    }

    /**
     * Fires every 3 600 000 ms (one hour).
     */
    @Scheduled(fixedRate = 3_600_000)
    public void runFullSync() {
        LOG.info("[ERP-SCHEDULER] Starting erpInitialImportJob ...");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(erpInitialImportJob, params);
            LOG.info("[ERP-SCHEDULER] erpInitialImportJob finished.");
        } catch (Exception ex) {
            // Log and swallow – the application must not crash because of a single
            // failed sync cycle.  Alerting is handled by log-based monitors.
            LOG.error("[ERP-SCHEDULER] erpInitialImportJob failed: {}", ex.getMessage(), ex);
        }
    }
}
