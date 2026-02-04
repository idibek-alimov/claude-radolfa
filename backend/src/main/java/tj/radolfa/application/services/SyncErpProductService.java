package tj.radolfa.application.services;

import org.springframework.stereotype.Service;

import tj.radolfa.application.ports.in.SyncErpProductUseCase;
import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.model.Product;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * Handles an inbound ERP sync event for a single product.
 *
 * Strategy:
 *   - Product exists  -> call enrichWithErpData() to overwrite locked fields, then save.
 *   - Product missing -> create a skeleton record with only ERP data; enrichment fields
 *                        stay empty until a content-team member fills them in.
 */
@Service
public class SyncErpProductService implements SyncErpProductUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    public SyncErpProductService(LoadProductPort loadProductPort,
                                 SaveProductPort saveProductPort) {
        this.loadProductPort = loadProductPort;
        this.saveProductPort = saveProductPort;
    }

    @Override
    public Product execute(String erpId, String name, BigDecimal price, Integer stock) {

        Product product = loadProductPort.load(erpId)
                .orElseGet(() -> new Product(
                        null,                    // id
                        erpId,
                        null,                    // name  – will be set below
                        null,                    // price – will be set below
                        null,                    // stock – will be set below
                        null,                    // webDescription – empty skeleton
                        false,                   // topSelling
                        Collections.emptyList()  // images
                ));

        // The single authorised write path for ERP-locked fields.
        product.enrichWithErpData(name, price, stock);

        return saveProductPort.save(product);
    }
}
