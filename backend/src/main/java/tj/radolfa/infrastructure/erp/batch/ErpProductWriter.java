package tj.radolfa.infrastructure.erp.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import tj.radolfa.domain.model.ProductTemplate;

/**
 * Spring Batch writer for the product sync pipeline.
 *
 * <p>The processor already persists via {@link tj.radolfa.application.ports.in.SyncProductUseCase},
 * so this writer only logs the results.
 */
@Component
public class ErpProductWriter implements ItemWriter<ProductTemplate> {

    private static final Logger LOG = LoggerFactory.getLogger(ErpProductWriter.class);

    @Override
    public void write(Chunk<? extends ProductTemplate> chunk) {
        for (ProductTemplate template : chunk) {
            LOG.debug("[BATCH-WRITER] Processed template={}", template.getErpTemplateCode());
        }
        LOG.info("[BATCH-WRITER] Wrote {} product templates", chunk.size());
    }
}
