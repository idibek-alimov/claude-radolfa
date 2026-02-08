package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdateProductUseCase;
import tj.radolfa.application.ports.out.ElasticsearchProductIndexer;
import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Product;

import java.util.List;

@Service
public class UpdateProductService implements UpdateProductUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;
    private final ElasticsearchProductIndexer indexer;

    public UpdateProductService(LoadProductPort loadProductPort,
                                SaveProductPort saveProductPort,
                                ElasticsearchProductIndexer indexer) {
        this.loadProductPort = loadProductPort;
        this.saveProductPort = saveProductPort;
        this.indexer = indexer;
    }

    @Override
    @Transactional
    public Product execute(String erpId, String name, Money price, Integer stock, String webDescription,
            boolean topSelling, List<String> images) {
        Product existing = loadProductPort.load(erpId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + erpId));

        Product updated = new Product(
                existing.getId(),
                existing.getErpId(),
                name,
                price,
                stock,
                webDescription,
                topSelling,
                images,
                existing.getLastErpSyncAt());

        Product saved = saveProductPort.save(updated);
        indexer.index(saved);
        return saved;
    }
}
