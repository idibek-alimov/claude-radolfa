package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdateListingUseCase;
import tj.radolfa.application.ports.out.LoadColorImagesPort;
import tj.radolfa.application.ports.out.LoadProductTemplatePort;
import tj.radolfa.application.ports.out.LoadProductVariantPort;
import tj.radolfa.application.ports.out.SaveColorImagesPort;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.model.ColorImages;
import tj.radolfa.domain.model.ProductTemplate;
import tj.radolfa.domain.model.ProductVariant;

@Service
@Transactional
public class UpdateListingService implements UpdateListingUseCase {

    private final LoadProductVariantPort loadVariantPort;
    private final LoadProductTemplatePort loadTemplatePort;
    private final SaveProductPort saveProductPort;
    private final LoadColorImagesPort loadColorImagesPort;
    private final SaveColorImagesPort saveColorImagesPort;

    public UpdateListingService(LoadProductVariantPort loadVariantPort,
                                LoadProductTemplatePort loadTemplatePort,
                                SaveProductPort saveProductPort,
                                LoadColorImagesPort loadColorImagesPort,
                                SaveColorImagesPort saveColorImagesPort) {
        this.loadVariantPort = loadVariantPort;
        this.loadTemplatePort = loadTemplatePort;
        this.saveProductPort = saveProductPort;
        this.loadColorImagesPort = loadColorImagesPort;
        this.saveColorImagesPort = saveColorImagesPort;
    }

    @Override
    public void update(String slug, UpdateListingCommand command) {
        ProductVariant variant = loadVariantPort.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + slug));

        ProductTemplate template = loadTemplatePort.findById(variant.getTemplateId())
                .orElseThrow(() -> new IllegalStateException("Template not found for variant: " + slug));

        if (command.webDescription() != null) {
            template.updateDescription(command.webDescription());
        }
        if (command.topSelling() != null) {
            template.updateTopSelling(command.topSelling());
        }
        if (command.featured() != null) {
            template.updateFeatured(command.featured());
        }

        saveProductPort.saveTemplate(template);
    }

    @Override
    public void addImage(String slug, String imageUrl) {
        ProductVariant variant = loadVariantPort.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + slug));

        ColorImages colorImages = loadOrCreate(variant);
        colorImages.addImage(imageUrl);
        saveColorImagesPort.save(colorImages);
    }

    @Override
    public void removeImage(String slug, String imageUrl) {
        ProductVariant variant = loadVariantPort.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + slug));

        ColorImages colorImages = loadOrCreate(variant);
        colorImages.removeImage(imageUrl);
        saveColorImagesPort.save(colorImages);
    }

    private ColorImages loadOrCreate(ProductVariant variant) {
        String colorKey = variant.getColor();
        return loadColorImagesPort.findByTemplateAndColor(variant.getTemplateId(), colorKey)
                .orElseGet(() -> new ColorImages(null, variant.getTemplateId(), colorKey, null));
    }
}
