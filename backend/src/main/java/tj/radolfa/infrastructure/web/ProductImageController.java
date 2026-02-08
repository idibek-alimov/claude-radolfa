package tj.radolfa.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.infrastructure.async.AsyncImageProcessor;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;
import tj.radolfa.infrastructure.web.dto.ProductImageResponseDto;

import java.io.IOException;
import java.util.List;

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
 * <h3>Async pipeline</h3>
 * <p>The endpoint buffers the raw bytes, validates inputs synchronously,
 * then delegates resize + S3 upload to a background thread pool.
 * Returns {@code 202 Accepted} immediately.
 *
 * <h3>Boundary validations (controller responsibility)</h3>
 * <ul>
 *   <li>Product must exist (fail-fast before going async).</li>
 *   <li>Content type must start with {@code image/}.</li>
 *   <li>File size must not exceed {@value #MAX_UPLOAD_SIZE_BYTES} bytes (5 MB).</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <p>This endpoint requires the {@code MANAGER} role.
 *
 * <h3>Critical Constraint</h3>
 * <p>MANAGER can only enrich products (images, descriptions). They CANNOT
 * modify ERP-controlled fields (price, name, stock).
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductImageController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductImageController.class);

    /** 5 MB hard cap on uploaded image payloads. */
    private static final long MAX_UPLOAD_SIZE_BYTES = 5L * 1024 * 1024;

    private final AsyncImageProcessor asyncImageProcessor;
    private final LoadProductPort loadProductPort;

    public ProductImageController(AsyncImageProcessor asyncImageProcessor,
                                  LoadProductPort loadProductPort) {
        this.asyncImageProcessor = asyncImageProcessor;
        this.loadProductPort = loadProductPort;
    }

    // ----------------------------------------------------------------
    // Endpoint
    // ----------------------------------------------------------------

    /**
     * Accepts an image upload for background processing.
     *
     * <p>Validates inputs synchronously, buffers raw bytes, fires async
     * processing, and returns {@code 202 Accepted} immediately.
     * The image URL will appear in the product's image list once
     * background processing completes.
     *
     * @param erpId the ERP identifier of the target product
     * @param file  the multipart image payload (part name {@code image})
     * @return 202 Accepted, or 400/404 on validation failure
     */
    @PostMapping("/{erpId}/images")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> uploadImage(
            @PathVariable          String        erpId,
            @RequestParam("image") MultipartFile file) {

        // --- fail-fast: product must exist before we go async ---
        if (loadProductPort.load(erpId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // --- boundary validations ---
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            LOG.warn("[IMAGE] Rejected upload for erpId={}: content type '{}' is not an image",
                    erpId, file.getContentType());
            return ResponseEntity.badRequest()
                    .body(new ProductImageResponseDto(erpId,
                            List.of("Error: content type must start with 'image/'")));
        }

        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            LOG.warn("[IMAGE] Rejected upload for erpId={}: size {} exceeds {} byte limit",
                    erpId, file.getSize(), MAX_UPLOAD_SIZE_BYTES);
            return ResponseEntity.badRequest()
                    .body(new ProductImageResponseDto(erpId,
                            List.of("Error: file size exceeds 5 MB limit")));
        }

        // --- buffer bytes and fire async ---
        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException ex) {
            LOG.error("[IMAGE] IO error reading multipart payload for erpId={}", erpId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponseDto.error("Failed to read uploaded file"));
        }

        asyncImageProcessor.processAsync(erpId, imageBytes, file.getOriginalFilename());

        LOG.info("[IMAGE] Accepted image for async processing: erpId={}, size={}KB",
                erpId, imageBytes.length / 1024);

        return ResponseEntity.accepted()
                .body(MessageResponseDto.success("Image accepted for background processing"));
    }
}
