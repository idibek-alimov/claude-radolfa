package tj.radolfa.infrastructure.erp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.ElasticsearchProductIndexer;
import tj.radolfa.domain.model.Product;

/**
 * No-op stub for {@link ElasticsearchProductIndexer}.
 *
 * Active only on {@code dev} and {@code test} profiles.
 * Simply logs the product that would be indexed.  The real ES adapter
 * is wired in a later phase.
 */
@Component
@Profile({"dev", "test"})
public class ElasticsearchProductIndexerStub implements ElasticsearchProductIndexer {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchProductIndexerStub.class);

    @Override
    public void index(Product product) {
        LOG.info("[ES-STUB] Would index product erpId={}, name={}", product.getErpId(), product.getName());
    }
}
