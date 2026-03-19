package tj.radolfa.infrastructure.importer;

import java.math.BigDecimal;

/**
 * Raw DTO that mirrors a single product record as returned by the external catalogue.
 *
 * Deliberately lives in {@code infrastructure}, not {@code domain} –
 * it is a transport object that exists only to carry wire-format data
 * from the import client into the application layer.
 */
public record ImportedProductSnapshot(
        String     importId,
        String     name,
        String     category,
        BigDecimal standardRate,
        Integer    stock,
        boolean    disabled
) {}
