package tj.radolfa.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import tj.radolfa.application.ports.in.AddProductImageUseCase;
import tj.radolfa.domain.exception.ErpLockViolationException;
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
 *   Header:       X-User-Role: MANAGER   (placeholder guard -- Phase 5 replaces with JWT)
 * </pre>
 *
 * <h3>Boundary validations (controller responsibility)</h3>
 * <ul>
 *   <li>Content type must start with {@code image/}.</li>
 *   <li>File size must not exceed {@value #MAX_UPLOAD_SIZE_BYTES} bytes (5 MB).</li>
 * </ul>
 *
 * <h3>Security (placeholder)</h3>
 * Until Phase 5 wires real JWT auth, callers must send the header
 * {@code X-User-Role: MANAGER}.  Any other value (or absence) results
 * in a {@code 403 Forbidden}, mirroring the {@code X-Sync-Role: SYSTEM}
 * guard on {@link ErpSyncController}.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductImageController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductImageController.class);

    /** 5 MB hard cap on uploaded image payloads. */
    private static final long MAX_UPLOAD_SIZE_BYTES = 5L * 1024 * 1024;

    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String REQUIRED_ROLE    = "MANAGER";

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
     * @param erpId the ERP identifier of the target product (path variable)
     * @param role  value of the {@code X-User-Role} request header
     * @param file  the multipart image payload (part name {@code image})
     * @return 201 Created with the updated image list, or an error status
     */
    @PostMapping("/{erpId}/images")
    public ResponseEntity<ProductImageResponseDto> uploadImage(
            @PathVariable          String        erpId,
            @RequestHeader(value = USER_ROLE_HEADER, defaultValue = "") String role,
            @RequestParam("image") MultipartFile file) {

        // --- role guard (Phase 5 placeholder) ---
        guardManagerRole(role);

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
    // Guard -- placeholder for Phase 5 auth
    // ----------------------------------------------------------------

    /**
     * Throws {@link ErpLockViolationException} (mapped to 403) when the
     * caller has not declared the {@code MANAGER} role via the header.
     */
    private void guardManagerRole(String role) {
        if (!REQUIRED_ROLE.equals(role)) {
            throw new ErpLockViolationException("image upload endpoint");
        }
    }

    // ----------------------------------------------------------------
    // Exception mapping (scoped to this controller only)
    // ----------------------------------------------------------------

    @ExceptionHandler(ErpLockViolationException.class)
    public ResponseEntity<String> handleLockViolation(ErpLockViolationException ex) {
        LOG.warn("[IMAGE] Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(ImageProcessingException.class)
    public ResponseEntity<String> handleImageProcessingError(ImageProcessingException ex) {
        LOG.error("[IMAGE] Processing failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
