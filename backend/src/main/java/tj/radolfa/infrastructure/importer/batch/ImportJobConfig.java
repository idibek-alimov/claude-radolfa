package tj.radolfa.infrastructure.importer.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import tj.radolfa.application.ports.in.sync.SyncProductHierarchyUseCase.HierarchySyncCommand;
import tj.radolfa.infrastructure.importer.ImportedProductSnapshot;

/**
 * Spring Batch {@code @Configuration} that assembles the
 * {@code productImportJob}.
 *
 * <p>Chunk size is 10: each transaction commits after 10 products are
 * read, processed, and written.
 */
@Configuration
public class ImportJobConfig {

    private static final int CHUNK_SIZE = 10;

    @Bean
    public Job productImportJob(JobRepository jobRepository,
                                Step productImportStep) {
        return new JobBuilder("productImportJob", jobRepository)
                .start(productImportStep)
                .build();
    }

    @Bean
    public Step productImportStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  ImportedProductReader    reader,
                                  ImportedProductProcessor processor,
                                  ImportedProductWriter    writer) {
        return new StepBuilder("productImportStep", jobRepository)
                .<ImportedProductSnapshot, HierarchySyncCommand>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
