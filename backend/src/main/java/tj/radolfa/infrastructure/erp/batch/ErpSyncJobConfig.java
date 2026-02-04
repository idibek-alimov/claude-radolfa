package tj.radolfa.infrastructure.erp.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import tj.radolfa.domain.model.Product;
import tj.radolfa.infrastructure.erp.ErpProductSnapshot;

/**
 * Spring Batch {@code @Configuration} that assembles the
 * {@code erpInitialImportJob}.
 *
 * Chunk size is 10: each transaction commits after 10 products are
 * read, processed, and written.
 */
@Configuration
public class ErpSyncJobConfig {

    private static final int CHUNK_SIZE = 10;

    // ----------------------------------------------------------------
    // Job
    // ----------------------------------------------------------------
    @Bean
    public Job erpInitialImportJob(JobRepository jobRepository,
                                   Step erpImportStep) {
        return new JobBuilder("erpInitialImportJob", jobRepository)
                .start(erpImportStep)
                .build();
    }

    // ----------------------------------------------------------------
    // Step
    // ----------------------------------------------------------------
    @Bean
    public Step erpImportStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              ErpProductReader   reader,
                              ErpProductProcessor processor,
                              ErpProductWriter   writer) {
        return new StepBuilder("erpImportStep", jobRepository)
                .<ErpProductSnapshot, Product>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
