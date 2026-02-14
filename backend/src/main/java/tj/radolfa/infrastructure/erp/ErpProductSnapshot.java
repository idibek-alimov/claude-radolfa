package tj.radolfa.infrastructure.erp;

import java.math.BigDecimal;

/**
 * Raw DTO that mirrors a single product record as returned by ERPNext.
 *
 * Deliberately lives in {@code infrastructure}, not {@code domain} –
 * it is a transport object that exists only to carry wire-format data
 * from the ERP client into the application layer.
 */
public record ErpProductSnapshot(
        String     erpId,
        String     name,
        String     category,
        BigDecimal price,
        Integer    stock
) {}
