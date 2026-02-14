package tj.radolfa.application.ports.in;

import java.util.List;

public interface UpdateListingUseCase {
    void update(String slug, UpdateListingCommand command);

    void addImage(String slug, String imageUrl);

    void removeImage(String slug, String imageUrl);

    record UpdateListingCommand(
            String webDescription,
            Boolean topSelling,
            Boolean featured,
            List<String> images) {
    }
}
