package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Product;

import java.io.InputStream;

/**
 * In-Port: upload an image for a product and attach the resulting URL
 * to the product's enrichment image list.
 */
public interface AddProductImageUseCase {

    /**
     * @param erpId         the ERP identifier of the target product
     * @param imageStream   raw upload bytes from the HTTP multipart payload
     * @param originalName  original filename (used to detect source format)
     * @return              the product with the new image URL appended
     */
    Product execute(String erpId, InputStream imageStream, String originalName);
}
