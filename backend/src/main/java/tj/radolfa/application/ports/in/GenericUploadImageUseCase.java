package tj.radolfa.application.ports.in;

import java.io.InputStream;

/**
 * In-Port: upload a raw image to generic media storage without any product context.
 *
 * <p>Used by the frontend "staged upload" flow — images are uploaded first,
 * URLs are collected, and then passed into product creation requests.</p>
 */
public interface GenericUploadImageUseCase {

    /**
     * @param imageStream      raw image bytes from the HTTP multipart upload
     * @param originalFilename original client filename (used to detect source format)
     * @return                 the public URL of the uploaded image
     */
    String upload(InputStream imageStream, String originalFilename);
}
