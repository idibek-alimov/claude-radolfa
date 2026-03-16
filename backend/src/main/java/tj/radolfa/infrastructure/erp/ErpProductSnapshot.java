package tj.radolfa.infrastructure.erp;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Raw DTO mirroring a single product record from ERPNext.
 *
 * <p>For template items (has_variants=1), {@code variants} is populated
 * with the child variant snapshots. For standalone items, {@code variants}
 * is empty and the template itself is the purchasable unit.
 *
 * <p>Lives in {@code infrastructure} — transport object only.
 */
public record ErpProductSnapshot(
        String              erpItemCode,
        String              name,
        String              category,
        BigDecimal          standardRate,
        Integer             stock,
        boolean             disabled,
        boolean             hasVariants,
        String              variantOf,
        Map<String, String> attributes
) {
    /** Convenience constructor for standalone/simple items. */
    public ErpProductSnapshot(String erpItemCode, String name, String category,
                              BigDecimal standardRate, Integer stock, boolean disabled) {
        this(erpItemCode, name, category, standardRate, stock, disabled,
             false, null, Map.of());
    }
}
