package tj.radolfa.application.ports.out;

import java.io.InputStream;

/**
 * The output of the image processing pipeline.
 * Immutable value object -- no setters, no mutation after construction.
 *
 * Intentionally a plain record: no Spring, no Jackson, no Lombok.
 */
public record ProcessedImage(
        InputStream data,
        String contentType,   // e.g. "image/webp" or "image/jpeg"
        String extension      // e.g. "webp" or "jpg"
) {}
