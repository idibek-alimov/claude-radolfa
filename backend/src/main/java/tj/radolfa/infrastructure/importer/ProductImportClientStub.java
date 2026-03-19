package tj.radolfa.infrastructure.importer;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * In-memory stub for {@link ProductImportClient}.
 *
 * Active only on the {@code dev} and {@code test} Spring profiles.
 * Returns a single page of three hard-coded sample products; every
 * subsequent page returns an empty list (pagination terminator).
 *
 * The real HTTP adapter that talks to the external catalogue is wired in production.
 */
@Component
@Profile({"dev", "test"})
public class ProductImportClientStub implements ProductImportClient {

    private static final List<ImportedProductSnapshot> PAGE_ONE = List.of(
            new ImportedProductSnapshot("SKU-001", "Radolfa T-Shirt",  "Tops",        new BigDecimal("29.99"), 50, false),
            new ImportedProductSnapshot("SKU-002", "Denim Jacket",     "Outerwear",   new BigDecimal("89.50"), 12, false),
            new ImportedProductSnapshot("SKU-003", "Summer Hat",       "Accessories", new BigDecimal("15.00"), 30, false)
    );

    @Override
    public List<ImportedProductSnapshot> fetchPage(int page, int limit) {
        // Only page 1 carries data; all further pages are empty (end signal).
        return page == 1 ? PAGE_ONE : List.of();
    }
}
