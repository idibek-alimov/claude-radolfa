package tj.radolfa.infrastructure.erp.batch;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.ElasticsearchProductIndexer;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.model.Product;

/**
 * Spring Batch {@code ItemWriter} that persists each product and then
 * pushes it into the search index.
 *
 * Note: {@link SaveProductPort#save} is called here even though
 * {@link tj.radolfa.application.services.SyncErpProductService} already saves.
 * The processor's save ensures the domain object is persisted; this writer's
 * save is idempotent (same entity, same state) and guarantees the post-process
 * write contract.  The index call is the primary added value of this writer.
 */
@Component
public class ErpProductWriter implements ItemWriter<Product> {

    private final SaveProductPort              savePort;
    private final ElasticsearchProductIndexer  indexer;

    public ErpProductWriter(SaveProductPort savePort,
                            ElasticsearchProductIndexer indexer) {
        this.savePort = savePort;
        this.indexer  = indexer;
    }

    @Override
    public void write(Chunk<? extends Product> chunk) {
        for (Product product : chunk) {
            Product persisted = savePort.save(product);
            indexer.index(persisted);
        }
    }
}
