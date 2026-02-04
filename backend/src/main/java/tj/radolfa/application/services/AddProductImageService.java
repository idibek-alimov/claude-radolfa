package tj.radolfa.application.services;

import org.springframework.stereotype.Service;

import tj.radolfa.application.ports.in.AddProductImageUseCase;
import tj.radolfa.application.ports.out.ImageProcessingPort;
import tj.radolfa.application.ports.out.ImageUploadPort;
import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.application.ports.out.ProcessedImage;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.exception.ErpLockViolationException;
import tj.radolfa.domain.model.Product;

import java.io.InputStream;
import java.util.UUID;

/**
 * Orchestrates the image-upload flow for a single product.
 *
 * Responsibilities (in order):
 *   1. Load the product by erpId -- fail fast if it does not exist.
 *   2. Delegate resize/compress to {@link ImageProcessingPort}.
 *   3. Generate the deterministic object key (naming convention lives here,
 *      not in the transport layer).
 *   4. Delegate the actual upload to {@link ImageUploadPort}.
 *   5. Mutate the domain aggregate via {@code Product.addImage(url)}.
 *   6. Persist and return the updated product.
 *
 * This class imports ONLY application ports and domain types.
 * No Thumbnailator. No S3Client. No framework beyond {@link Service}.
 */
@Service
public class AddProductImageService implements AddProductImageUseCase {

    private final LoadProductPort      loadProductPort;
    private final SaveProductPort      saveProductPort;
    private final ImageProcessingPort  imageProcessingPort;
    private final ImageUploadPort      imageUploadPort;

    public AddProductImageService(LoadProductPort     loadProductPort,
                                  SaveProductPort     saveProductPort,
                                  ImageProcessingPort imageProcessingPort,
                                  ImageUploadPort     imageUploadPort) {
        this.loadProductPort     = loadProductPort;
        this.saveProductPort     = saveProductPort;
        this.imageProcessingPort = imageProcessingPort;
        this.imageUploadPort     = imageUploadPort;
    }

    @Override
    public Product execute(String erpId, InputStream imageStream, String originalName) {

        // 1 -- load; the product must already exist (created by ERP sync)
        Product product = loadProductPort.load(erpId)
                .orElseThrow(() -> new ErpLockViolationException(
                        "product '" + erpId + "' does not exist -- cannot attach image"));

        // 2 -- process (resize + compress); result is format-agnostic
        ProcessedImage processed = imageProcessingPort.process(imageStream, originalName);

        // 3 -- generate the object key: products/{erpId}/{uuid}.{ext}
        String objectKey = "products/" + erpId + "/" + UUID.randomUUID() + "." + processed.extension();

        // 4 -- upload; returns the public URL
        String publicUrl = imageUploadPort.upload(processed.data(), objectKey, processed.contentType());

        // 5 -- domain mutation (enrichment path)
        product.addImage(publicUrl);

        // 6 -- persist and return
        return saveProductPort.save(product);
    }
}
