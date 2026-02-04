package tj.radolfa.application.ports.out;

import java.io.InputStream;

/**
 * Out-Port: upload a processed image and return its public URL.
 *
 * The port knows nothing about S3, file formats, or compression --
 * those are infrastructure concerns handled by the adapter.
 */
public interface ImageUploadPort {

    /**
     * @param imageStream   the already-processed (resized, compressed) image bytes
     * @param objectKey     the desired storage key (e.g. "products/SKU-001/abc123.webp")
     * @param contentType   MIME type (e.g. "image/webp", "image/jpeg")
     * @return              the public URL of the uploaded object
     */
    String upload(InputStream imageStream, String objectKey, String contentType);
}
