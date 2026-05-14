package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.GenericUploadImageUseCase;
import tj.radolfa.application.ports.out.ImageProcessingPort;
import tj.radolfa.application.ports.out.ImageUploadPort;
import tj.radolfa.application.ports.out.ProcessedImage;

import java.io.InputStream;
import java.util.UUID;

/**
 * Processes and uploads an image to a permanent generic media path in S3.
 * No product slug required — no DB write performed.
 *
 * <p>Object key format: {@code uploads/media/{UUID}.{extension}}</p>
 */
@Service
public class GenericUploadImageService implements GenericUploadImageUseCase {

    private final ImageProcessingPort imageProcessingPort;
    private final ImageUploadPort     imageUploadPort;

    public GenericUploadImageService(ImageProcessingPort imageProcessingPort,
                                     ImageUploadPort imageUploadPort) {
        this.imageProcessingPort = imageProcessingPort;
        this.imageUploadPort     = imageUploadPort;
    }

    @Override
    public String upload(InputStream imageStream, String originalFilename) {
        ProcessedImage processed = imageProcessingPort.process(imageStream, originalFilename);

        String objectKey = "uploads/media/" + UUID.randomUUID() + "." + processed.extension();
        return imageUploadPort.upload(processed.data(), objectKey, processed.contentType());
    }
}
