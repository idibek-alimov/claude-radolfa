package tj.radolfa.application.ports.out;

import java.io.InputStream;

/**
 * Out-Port: resize and compress an image according to pipeline rules.
 * Returns a processed image ready for upload.
 *
 * The port is intentionally agnostic to the underlying library
 * (Thumbnailator, ImageMagick, etc.) -- that is an infrastructure choice.
 */
public interface ImageProcessingPort {

    /**
     * @param source           raw image bytes as received from the client
     * @param originalFilename original filename; may be used to detect the source format
     * @return a {@link ProcessedImage} containing the resized/compressed bytes,
     *         the target MIME type, and the file extension
     */
    ProcessedImage process(InputStream source, String originalFilename);
}
