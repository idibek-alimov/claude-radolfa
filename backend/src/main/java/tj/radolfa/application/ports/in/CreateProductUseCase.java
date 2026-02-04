package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Product;

import java.util.List;

/**
 * In-Port: create a new product record.
 *
 * Only enrichment fields are supplied here.  The ERP-locked fields
 * (name, price, stock) arrive later via {@link SyncErpProductUseCase}.
 */
public interface CreateProductUseCase {

    /**
     * @param erpId          unique identifier assigned by ERPNext
     * @param webDescription marketing copy owned by Radolfa
     * @param topSelling     curated flag owned by Radolfa
     * @param images         list of S3 URLs for product images
     * @return the persisted domain Product
     */
    Product execute(String erpId,
                    String webDescription,
                    boolean topSelling,
                    List<String> images);
}
