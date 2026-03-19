package tj.radolfa.infrastructure.importer.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.in.sync.SyncProductHierarchyUseCase;
import tj.radolfa.application.ports.in.sync.SyncProductHierarchyUseCase.HierarchySyncCommand;

/**
 * Spring Batch {@code ItemWriter} for the hierarchy import pipeline.
 *
 * <p>Delegates each hierarchy command to the use case for persistence.
 */
@Component
public class ImportedProductWriter implements ItemWriter<HierarchySyncCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(ImportedProductWriter.class);

    private final SyncProductHierarchyUseCase syncUseCase;

    public ImportedProductWriter(SyncProductHierarchyUseCase syncUseCase) {
        this.syncUseCase = syncUseCase;
    }

    @Override
    public void write(Chunk<? extends HierarchySyncCommand> chunk) {
        for (HierarchySyncCommand command : chunk) {
            syncUseCase.execute(command);
            LOG.debug("[BATCH-WRITER] Synced template={}", command.templateCode());
        }
        LOG.info("[BATCH-WRITER] Wrote {} product hierarchies", chunk.size());
    }
}
