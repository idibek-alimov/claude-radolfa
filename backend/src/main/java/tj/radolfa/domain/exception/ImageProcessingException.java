package tj.radolfa.domain.exception;

/**
 * Thrown when the image processing pipeline receives input it cannot handle
 * (corrupt bytes, unsupported format, IO failure during resize).
 *
 * Lives in {@code domain.exception} because it is a domain-level failure signal:
 * the image data itself is invalid, independent of any infrastructure detail.
 * Zero framework imports.
 */
public class ImageProcessingException extends RuntimeException {

    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
