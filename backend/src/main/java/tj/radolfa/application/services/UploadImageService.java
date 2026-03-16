package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UploadImageUseCase;
import tj.radolfa.application.ports.out.ImageProcessingPort;
import tj.radolfa.application.ports.out.ImageUploadPort;
import tj.radolfa.application.ports.out.LoadColorImagesPort;
import tj.radolfa.application.ports.out.LoadProductVariantPort;
import tj.radolfa.application.ports.out.ProcessedImage;
import tj.radolfa.application.ports.out.SaveColorImagesPort;
import tj.radolfa.domain.model.ColorImages;
import tj.radolfa.domain.model.ProductVariant;

import java.io.InputStream;
import java.util.UUID;

@Service
@Transactional
public class UploadImageService implements UploadImageUseCase {

    private final LoadProductVariantPort loadVariantPort;
    private final LoadColorImagesPort loadColorImagesPort;
    private final SaveColorImagesPort saveColorImagesPort;
    private final ImageProcessingPort imageProcessingPort;
    private final ImageUploadPort imageUploadPort;

    public UploadImageService(LoadProductVariantPort loadVariantPort,
                              LoadColorImagesPort loadColorImagesPort,
                              SaveColorImagesPort saveColorImagesPort,
                              ImageProcessingPort imageProcessingPort,
                              ImageUploadPort imageUploadPort) {
        this.loadVariantPort = loadVariantPort;
        this.loadColorImagesPort = loadColorImagesPort;
        this.saveColorImagesPort = saveColorImagesPort;
        this.imageProcessingPort = imageProcessingPort;
        this.imageUploadPort = imageUploadPort;
    }

    @Override
    public String upload(String slug, InputStream imageStream, String originalFilename) {
        ProductVariant variant = loadVariantPort.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + slug));

        ProcessedImage processed = imageProcessingPort.process(imageStream, originalFilename);

        String objectKey = "products/" + slug + "/" + UUID.randomUUID() + "." + processed.extension();
        String publicUrl = imageUploadPort.upload(processed.data(), objectKey, processed.contentType());

        ColorImages colorImages = loadOrCreate(variant);
        colorImages.addImage(publicUrl);
        saveColorImagesPort.save(colorImages);

        return publicUrl;
    }

    private ColorImages loadOrCreate(ProductVariant variant) {
        String colorKey = variant.getColor();
        return loadColorImagesPort.findByTemplateAndColor(variant.getTemplateId(), colorKey)
                .orElseGet(() -> new ColorImages(null, variant.getTemplateId(), colorKey, null));
    }
}
