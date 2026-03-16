package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ProductVariant;

import java.util.Optional;

public interface LoadProductVariantPort {

    Optional<ProductVariant> findByErpVariantCode(String erpVariantCode);

    Optional<ProductVariant> findBySlug(String slug);
}
