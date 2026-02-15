package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ListingVariant;

public interface SaveListingVariantPort {

    void save(ListingVariant variant);
}
