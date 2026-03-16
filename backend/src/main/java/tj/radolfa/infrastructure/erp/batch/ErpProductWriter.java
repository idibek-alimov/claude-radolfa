package tj.radolfa.infrastructure.erp.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand;

/**
 * Spring Batch {@code ItemWriter} for the hierarchy sync pipeline.
 *
 * <p>Delegates each hierarchy command to the use case for persistence.
 */
@Component
public class ErpProductWriter implements ItemWriter<HierarchySyncCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(ErpProductWriter.class);

    private final SyncProductHierarchyUseCase syncUseCase;

    public ErpProductWriter(SyncProductHierarchyUseCase syncUseCase) {
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
