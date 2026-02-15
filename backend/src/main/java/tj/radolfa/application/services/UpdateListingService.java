package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdateListingUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.SaveListingVariantPort;
import tj.radolfa.domain.model.ListingVariant;

@Service
@Transactional
public class UpdateListingService implements UpdateListingUseCase {

    private final LoadListingVariantPort loadListingVariantPort;
    private final SaveListingVariantPort saveListingVariantPort;

    public UpdateListingService(LoadListingVariantPort loadListingVariantPort,
                                SaveListingVariantPort saveListingVariantPort) {
        this.loadListingVariantPort = loadListingVariantPort;
        this.saveListingVariantPort = saveListingVariantPort;
    }

    @Override
    public void update(String slug, UpdateListingCommand command) {
        ListingVariant variant = loadListingVariantPort.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + slug));

        if (command.webDescription() != null) {
            variant.updateWebDescription(command.webDescription());
        }
        if (command.topSelling() != null) {
            variant.updateTopSelling(command.topSelling());
        }
        if (command.featured() != null) {
            variant.updateFeatured(command.featured());
        }

        saveListingVariantPort.save(variant);
    }

    @Override
    public void addImage(String slug, String imageUrl) {
        ListingVariant variant = loadListingVariantPort.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + slug));

        variant.addImage(imageUrl);
        saveListingVariantPort.save(variant);
    }

    @Override
    public void removeImage(String slug, String imageUrl) {
        ListingVariant variant = loadListingVariantPort.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + slug));

        variant.removeImage(imageUrl);
        saveListingVariantPort.save(variant);
    }
}
