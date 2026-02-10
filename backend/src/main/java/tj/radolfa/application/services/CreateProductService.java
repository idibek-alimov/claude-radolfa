package tj.radolfa.application.services;

import org.springframework.stereotype.Service;

import tj.radolfa.application.ports.in.CreateProductUseCase;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Product;

import java.util.List;

/**
 * Orchestrates the creation of a new product record.
 *
 * Only enrichment fields are written. ERP-locked fields (name, price, stock)
 * remain null until the first ERP sync cycle completes.
 */
@Service
public class CreateProductService implements CreateProductUseCase {

    private final SaveProductPort saveProductPort;

    public CreateProductService(SaveProductPort saveProductPort) {
        this.saveProductPort = saveProductPort;
    }

    @Override
    public Product execute(String erpId,
            String name,
            Money price,
            Integer stock,
            String webDescription,
            boolean topSelling,
            List<String> images) {

        Product product = new Product(
                null, // id – assigned by DB
                erpId,
                name,
                price,
                stock,
                webDescription,
                topSelling,
                images,
                null // lastErpSyncAt – stamped on first sync
        );

        return saveProductPort.save(product);
    }
}
