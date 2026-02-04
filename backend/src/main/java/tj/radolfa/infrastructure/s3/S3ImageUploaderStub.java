package tj.radolfa.infrastructure.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.ImageUploadPort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Development / test stub for {@link ImageUploadPort}.
 *
 * Does NOT contact AWS.  Writes the image bytes to a local
 * {@code ./uploads/} directory (relative to the working directory at
 * startup) and returns an {@code http://localhost:8080/uploads/...} URL
 * so that the full pipeline can be exercised without an AWS account.
 *
 * Activated under the {@code dev} and {@code test} profiles.
 * The production adapter ({@link S3ImageUploader}) is activated under {@code prod}.
 */
@Component
@Profile({"dev", "test"})
public class S3ImageUploaderStub implements ImageUploadPort {

    private static final Logger LOG = LoggerFactory.getLogger(S3ImageUploaderStub.class);

    /** Root directory for locally-written images. */
    private static final Path UPLOADS_ROOT = Path.of("uploads");

    @Override
    public String upload(InputStream imageStream, String objectKey, String contentType) {
        try {
            // Ensure the full directory tree exists (e.g. uploads/products/SKU-001/)
            Path target = UPLOADS_ROOT.resolve(objectKey);
            Files.createDirectories(target.getParent());

            // Write bytes to disk
            byte[] bytes = imageStream.readAllBytes();
            Files.write(target, bytes);

            LOG.info("[S3-STUB] Wrote {} bytes to {}", bytes.length, target.toAbsolutePath());

            // Return a localhost URL that mirrors what a real S3 public URL would look like
            return "http://localhost:8080/uploads/" + objectKey;

        } catch (IOException ex) {
            throw new RuntimeException("S3 stub: failed to write image to local filesystem", ex);
        }
    }
}
