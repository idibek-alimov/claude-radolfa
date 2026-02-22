package tj.radolfa.application.ports.in;

import java.io.InputStream;

/**
 * In-Port: receive a raw image file, process it, upload to storage,
 * and attach the resulting public URL to a listing variant.
 *
 * <p>Callers (controllers) hand off the raw bytes; this use case owns
 * the full pipeline: process → upload → persist URL.</p>
 */
public interface UploadImageUseCase {

    /**
     * @param slug             slug of the listing variant to attach the image to
     * @param imageStream      raw image bytes from the HTTP multipart upload
     * @param originalFilename original client filename (used to detect source format)
     * @return                 the public URL of the uploaded image
     */
    String upload(String slug, InputStream imageStream, String originalFilename);
}
