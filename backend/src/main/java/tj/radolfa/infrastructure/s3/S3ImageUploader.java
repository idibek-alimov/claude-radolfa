package tj.radolfa.infrastructure.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import tj.radolfa.application.ports.out.ImageUploadPort;

import java.io.IOException;
import java.io.InputStream;

/**
 * Production adapter: uploads processed images to AWS S3.
 *
 * Activated only under the {@code prod} profile.  For local development
 * and tests, {@link S3ImageUploaderStub} takes over.
 *
 * This is the ONLY class in the project that imports {@code software.amazon.awssdk}.
 * The bucket name is injected from {@code aws.s3.bucket}, which itself reads
 * the {@code AWS_S3_BUCKET} environment variable -- no key or secret is ever
 * hard-coded or written to a file.
 */
@Component
@Profile("prod")
public class S3ImageUploader implements ImageUploadPort {

    private final S3Client s3;
    private final String   bucket;

    public S3ImageUploader(@Value("${aws.s3.bucket}") String bucket) {
        this.bucket = bucket;
        this.s3     = S3Client.builder().build();   // default credential chain (env / IAM role)
    }

    @Override
    public String upload(InputStream imageStream, String objectKey, String contentType) {
        byte[] bytes;
        try {
            bytes = imageStream.readAllBytes();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read image stream for upload", ex);
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3.putObject(request, RequestBody.fromBytes(bytes));

        // Standard virtual-hosted-style public URL
        return "https://" + bucket + ".s3.amazonaws.com/" + objectKey;
    }
}
