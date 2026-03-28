package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.AssignVariantTagsUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductTagPort;
import tj.radolfa.application.ports.out.SaveListingVariantPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductTag;

import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class AssignVariantTagsService implements AssignVariantTagsUseCase {

    private final LoadListingVariantPort loadListingVariantPort;
    private final LoadProductTagPort loadProductTagPort;
    private final SaveListingVariantPort saveListingVariantPort;

    public AssignVariantTagsService(LoadListingVariantPort loadListingVariantPort,
                                    LoadProductTagPort loadProductTagPort,
                                    SaveListingVariantPort saveListingVariantPort) {
        this.loadListingVariantPort = loadListingVariantPort;
        this.loadProductTagPort = loadProductTagPort;
        this.saveListingVariantPort = saveListingVariantPort;
    }

    @Override
    public void execute(Long variantId, List<Long> tagIds) {
        loadListingVariantPort.findVariantById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ListingVariant not found: id=" + variantId));

        if (tagIds != null && !tagIds.isEmpty()) {
            List<ProductTag> found = loadProductTagPort.findAllByIds(tagIds);
            if (found.size() != new HashSet<>(tagIds).size()) {
                throw new ResourceNotFoundException("One or more tag IDs not found: " + tagIds);
            }
        }

        saveListingVariantPort.saveTags(variantId, tagIds);
    }
}
