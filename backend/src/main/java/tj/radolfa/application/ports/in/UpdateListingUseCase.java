package tj.radolfa.application.ports.in;

import java.util.List;

public interface UpdateListingUseCase {
    void update(String slug, UpdateListingCommand command);

    void addImage(String slug, String imageUrl);

    void removeImage(String slug, String imageUrl);

    void reorderImages(String slug, List<String> orderedUrls);

    record UpdateListingCommand(
            String webDescription,
            Boolean topSelling,
            Boolean featured,
            Boolean active,
            List<String> images,
            List<AttributeEntry> attributes) {
    }

    record AttributeEntry(String key, String value) {}
}
