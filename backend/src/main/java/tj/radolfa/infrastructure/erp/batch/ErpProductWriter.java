package tj.radolfa.infrastructure.erp.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import tj.radolfa.domain.model.ProductBase;

/**
 * Spring Batch {@code ItemWriter} for the hierarchy sync pipeline.
 *
 * <p>The processor already persists data via
 * {@link tj.radolfa.application.ports.in.SyncProductHierarchyUseCase},
 * so this writer only handles post-processing (logging).
 *
 * <p>Elasticsearch re-indexing for the hierarchy model will be added
 * when the search adapter is migrated to the new schema.
 */
@Component
public class ErpProductWriter implements ItemWriter<ProductBase> {

    private static final Logger LOG = LoggerFactory.getLogger(ErpProductWriter.class);

    @Override
    public void write(Chunk<? extends ProductBase> chunk) {
        for (ProductBase base : chunk) {
            LOG.debug("[BATCH-WRITER] Processed template={}", base.getErpTemplateCode());
        }
        LOG.info("[BATCH-WRITER] Wrote {} product bases", chunk.size());
    }
}
