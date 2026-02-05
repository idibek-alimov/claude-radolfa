package tj.radolfa.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import tj.radolfa.application.ports.in.AddProductImageUseCase;
import tj.radolfa.domain.exception.ImageProcessingException;
import tj.radolfa.domain.model.Product;
import tj.radolfa.infrastructure.web.dto.ProductImageResponseDto;

/**
 * REST adapter for the product-image upload pipeline.
 *
 * <pre>
 *   POST /api/v1/products/{erpId}/images
 *   Content-Type: multipart/form-data
 *   Part name:    image
 *   Authorization: Bearer &lt;JWT token&gt;
 * </pre>
 *
 * <h3>Boundary validations (controller responsibility)</h3>
 * <ul>
 *   <li>Content type must start with {@code image/}.</li>
 *   <li>File size must not exceed {@value #MAX_UPLOAD_SIZE_BYTES} bytes (5 MB).</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <p>This endpoint requires the {@code MANAGER} role. The role is verified
 * via JWT token in the Authorization header.
 *
 * <h3>Critical Constraint</h3>
 * <p>MANAGER can only enrich products (images, descriptions). They CANNOT
 * modify ERP-controlled fields (price, name, stock). ERPNext is the SOURCE
 * OF TRUTH for those fields.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductImageController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductImageController.class);

    /** 5 MB hard cap on uploaded image payloads. */
    private static final long MAX_UPLOAD_SIZE_BYTES = 5L * 1024 * 1024;

    private final AddProductImageUseCase addProductImageUseCase;

    public ProductImageController(AddProductImageUseCase addProductImageUseCase) {
        this.addProductImageUseCase = addProductImageUseCase;
    }

    // ----------------------------------------------------------------
    // Endpoint
    // ----------------------------------------------------------------

    /**
     * Uploads an image for the product identified by {@code erpId},
     * runs it through the resize/compress pipeline, stores the result,
     * and appends the public URL to the product's image list.
     *
     * <p>Requires MANAGER role (enforced by Spring Security filter chain
     * and method-level security annotation).
     *
     * @param erpId the ERP identifier of the target product (path variable)
     * @param file  the multipart image payload (part name {@code image})
     * @return 201 Created with the updated image list, or an error status
     */
    @PostMapping("/{erpId}/images")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ProductImageResponseDto> uploadImage(
            @PathVariable          String        erpId,
            @RequestParam("image") MultipartFile file) {

        // --- boundary validations ---
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            LOG.warn("[IMAGE] Rejected upload for erpId={}: content type '{}' is not an image",
                    erpId, file.getContentType());
            return ResponseEntity.badRequest()
                    .body(new ProductImageResponseDto(erpId,
                            java.util.List.of("Error: content type must start with 'image/'")));
        }

        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            LOG.warn("[IMAGE] Rejected upload for erpId={}: size {} exceeds {} byte limit",
                    erpId, file.getSize(), MAX_UPLOAD_SIZE_BYTES);
            return ResponseEntity.badRequest()
                    .body(new ProductImageResponseDto(erpId,
                            java.util.List.of("Error: file size exceeds 5 MB limit")));
        }

        // --- delegate to use case ---
        try {
            Product updated = addProductImageUseCase.execute(
                    erpId,
                    file.getInputStream(),
                    file.getOriginalFilename()
            );

            LOG.info("[IMAGE] Successfully uploaded image for erpId={}, total images={}",
                    erpId, updated.getImages().size());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ProductImageResponseDto(updated.getErpId(), updated.getImages()));

        } catch (java.io.IOException ex) {
            LOG.error("[IMAGE] IO error reading multipart payload for erpId={}", erpId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ProductImageResponseDto(erpId,
                            java.util.List.of("Error: failed to read uploaded file")));
        }
    }

    // ----------------------------------------------------------------
    // Exception mapping (scoped to this controller only)
    // ----------------------------------------------------------------

    @ExceptionHandler(ImageProcessingException.class)
    public ResponseEntity<String> handleImageProcessingError(ImageProcessingException ex) {
        LOG.error("[IMAGE] Processing failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
