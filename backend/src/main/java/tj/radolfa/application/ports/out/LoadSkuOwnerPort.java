package tj.radolfa.application.ports.out;

import java.util.Optional;

/**
 * Out-Port: resolve the seller owner of a SKU.
 *
 * <p>The ownership chain is: {@code skus → listing_variants → product_bases.seller_id}.
 *
 * <ul>
 *   <li>{@code Optional.empty()} — SKU not found; callers should throw
 *       {@code IllegalArgumentException("SKU not found")}.</li>
 *   <li>{@code SkuOwner.sellerId() == null} — SKU exists but is Radolfa-owned
 *       ({@code product_bases.seller_id IS NULL}).</li>
 *   <li>{@code SkuOwner.sellerId() != null} — SKU belongs to that seller.</li>
 * </ul>
 */
public interface LoadSkuOwnerPort {

    Optional<SkuOwner> findBySkuId(Long skuId);

    record SkuOwner(Long skuId, Long sellerId) {}
}
