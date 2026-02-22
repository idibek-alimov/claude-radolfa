package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdateListingUseCase;
import tj.radolfa.application.ports.in.UploadImageUseCase;
import tj.radolfa.application.ports.out.ImageProcessingPort;
import tj.radolfa.application.ports.out.ImageUploadPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.ProcessedImage;
import tj.radolfa.application.ports.out.SaveListingVariantPort;
import tj.radolfa.domain.model.ListingVariant;

import java.io.InputStream;
import java.util.UUID;

@Service
@Transactional
public class UploadImageService implements UploadImageUseCase {

    private final LoadListingVariantPort loadListingVariantPort;
    private final SaveListingVariantPort saveListingVariantPort;
    private final ImageProcessingPort imageProcessingPort;
    private final ImageUploadPort imageUploadPort;

    public UploadImageService(LoadListingVariantPort loadListingVariantPort,
                              SaveListingVariantPort saveListingVariantPort,
                              ImageProcessingPort imageProcessingPort,
                              ImageUploadPort imageUploadPort) {
        this.loadListingVariantPort = loadListingVariantPort;
        this.saveListingVariantPort = saveListingVariantPort;
        this.imageProcessingPort = imageProcessingPort;
        this.imageUploadPort = imageUploadPort;
    }

    @Override
    public String upload(String slug, InputStream imageStream, String originalFilename) {
        ListingVariant variant = loadListingVariantPort.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + slug));

        ProcessedImage processed = imageProcessingPort.process(imageStream, originalFilename);

        String objectKey = "products/" + slug + "/" + UUID.randomUUID() + "." + processed.extension();
        String publicUrl = imageUploadPort.upload(processed.data(), objectKey, processed.contentType());

        variant.addImage(publicUrl);
        saveListingVariantPort.save(variant);

        return publicUrl;
    }
}
