package tj.radolfa.infrastructure.importer.batch;

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
 * Programmatically launches {@code productImportJob} so that any
 * products added or updated in the external catalogue since the last run
 * are pulled into Radolfa.
 *
 * A unique {@code run.id} parameter is injected into every execution
 * so that Spring Batch does not skip the job as a duplicate.
 *
 * TODO(pre-prod): This scheduler is restricted to dev/test profiles.
 *  Before going to production, decide on a reconciliation strategy:
 *  - Enable this scheduler in prod (possibly with a longer interval)
 *  - Or add a manual trigger endpoint for ops
 *  Without it, production relies solely on webhooks for product sync.
 */
@Component
@Profile({ "dev", "test" })
public class ImportJobScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ImportJobScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job productImportJob;

    public ImportJobScheduler(JobLauncher jobLauncher, Job productImportJob) {
        this.jobLauncher = jobLauncher;
        this.productImportJob = productImportJob;
    }

    /**
     * Fires every 3 600 000 ms (one hour).
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void runFullSync() {
        LOG.info("[IMPORT-SCHEDULER] Starting productImportJob ...");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(productImportJob, params);
            LOG.info("[IMPORT-SCHEDULER] productImportJob finished.");
        } catch (Exception ex) {
            // Log and swallow – the application must not crash because of a single
            // failed import cycle. Alerting is handled by log-based monitors.
            LOG.error("[IMPORT-SCHEDULER] productImportJob failed: {}", ex.getMessage(), ex);
        }
    }
}
