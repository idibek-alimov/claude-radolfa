package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.ProductAttribute;

import java.util.List;

public interface UpdateListingUseCase {
    void update(String slug, UpdateListingCommand command);

    void updateDimensions(String slug, UpdateDimensionsCommand command);

    void addImage(String slug, String imageUrl);

    void removeImage(String slug, String imageUrl);

    record UpdateListingCommand(
            String webDescription,
            List<ProductAttribute> attributes) {
    }

    record UpdateDimensionsCommand(
            Double  weightKg,
            Integer widthCm,
            Integer heightCm,
            Integer depthCm) {
    }
}
