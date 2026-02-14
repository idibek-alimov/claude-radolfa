package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdateListingUseCase;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;

import java.util.List;

@Service
@Transactional
public class UpdateListingService implements UpdateListingUseCase {

    private final ListingVariantRepository repository;

    public UpdateListingService(ListingVariantRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(String slug, UpdateListingCommand command) {
        ListingVariantEntity entity = repository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + slug));

        if (command.webDescription() != null) {
            entity.setWebDescription(command.webDescription());
        }
        if (command.topSelling() != null) {
            entity.setTopSelling(command.topSelling());
        }
        if (command.featured() != null) {
            entity.setFeatured(command.featured());
        }

        // We do not replace images here — image management is done via
        // addImage/removeImage
        // or by a dedicated reorder endpoint if needed.

        repository.save(entity);
    }

    @Override
    public void addImage(String slug, String imageUrl) {
        ListingVariantEntity entity = repository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + slug));

        int nextSortOrder = entity.getImages().stream()
                .mapToInt(ListingVariantImageEntity::getSortOrder)
                .max()
                .orElse(0) + 1;

        ListingVariantImageEntity image = new ListingVariantImageEntity();
        image.setListingVariant(entity);
        image.setImageUrl(imageUrl);
        image.setSortOrder(nextSortOrder);

        entity.getImages().add(image);
        repository.save(entity);
    }

    @Override
    public void removeImage(String slug, String imageUrl) {
        ListingVariantEntity entity = repository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + slug));

        entity.getImages().removeIf(img -> img.getImageUrl().equals(imageUrl));
        repository.save(entity);
    }
}
