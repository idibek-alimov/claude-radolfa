package tj.radolfa.application.ports.in;

import java.util.List;

public interface AssignVariantTagsUseCase {
    /**
     * Replaces all tags on the given variant with the provided list.
     * Pass an empty list to remove all tags.
     */
    void execute(Long variantId, List<Long> tagIds);
}
