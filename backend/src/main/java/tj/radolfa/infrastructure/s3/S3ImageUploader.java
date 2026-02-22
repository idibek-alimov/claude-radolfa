package tj.radolfa.infrastructure.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import tj.radolfa.application.ports.out.ImageUploadPort;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Production adapter: uploads processed images to any S3-compatible storage.
 *
 * Activated only under the {@code prod} profile. For local development
 * and tests, {@link S3ImageUploaderStub} takes over.
 *
 * Configured entirely from environment variables — no credentials are
 * hard-coded. Set these in your .env or deployment environment:
 *
 * <pre>
 *   AWS_S3_ENDPOINT=https://s3.twcstorage.ru
 *   AWS_S3_REGION=ru-1
 *   AWS_S3_BUCKET=your-bucket-name
 *   AWS_ACCESS_KEY_ID=your-access-key
 *   AWS_SECRET_ACCESS_KEY=your-secret-key
 * </pre>
 */
@Component
@Profile("prod")
public class S3ImageUploader implements ImageUploadPort {

    private final S3Client s3;
    private final String   bucket;
    private final String   endpointHost;

    public S3ImageUploader(
            @Value("${aws.s3.bucket}")     String bucket,
            @Value("${aws.s3.region}")     String region,
            @Value("${aws.s3.endpoint}")   String endpoint,
            @Value("${aws.s3.access-key}") String accessKey,
            @Value("${aws.s3.secret-key}") String secretKey) {

        this.bucket       = bucket;
        this.endpointHost = URI.create(endpoint).getHost(); // e.g. "s3.twcstorage.ru"

        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
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

        // Virtual-hosted-style public URL: https://{bucket}.{endpoint-host}/{key}
        // e.g. https://my-bucket.s3.twcstorage.ru/products/slug/uuid.webp
        return "https://" + bucket + "." + endpointHost + "/" + objectKey;
    }
}
