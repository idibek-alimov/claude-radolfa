package tj.radolfa.infrastructure.erp;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * In-memory stub for {@link ErpProductClient}.
 *
 * <p>Active on {@code dev} and {@code test} profiles.
 * Returns a realistic 2-layer product catalog with templates and variants.
 */
@Component
@Profile({"dev", "test"})
public class ErpProductClientStub implements ErpProductClient {

    private static final List<ErpProductSnapshot> PAGE_ONE = List.of(
            // Template: T-Shirt with variants
            new ErpProductSnapshot("TSHIRT-001", "Radolfa T-Shirt", "Tops",
                    BigDecimal.ZERO, 0, false, true, null, Map.of()),
            new ErpProductSnapshot("TSHIRT-001-RED-S", "Radolfa T-Shirt - Red - S", "Tops",
                    new BigDecimal("29.99"), 50, false, false, "TSHIRT-001",
                    Map.of("Color", "Red", "Size", "S")),
            new ErpProductSnapshot("TSHIRT-001-RED-M", "Radolfa T-Shirt - Red - M", "Tops",
                    new BigDecimal("29.99"), 30, false, false, "TSHIRT-001",
                    Map.of("Color", "Red", "Size", "M")),
            new ErpProductSnapshot("TSHIRT-001-BLUE-S", "Radolfa T-Shirt - Blue - S", "Tops",
                    new BigDecimal("29.99"), 20, false, false, "TSHIRT-001",
                    Map.of("Color", "Blue", "Size", "S")),

            // Template: Denim Jacket with variants
            new ErpProductSnapshot("JACKET-001", "Denim Jacket", "Outerwear",
                    BigDecimal.ZERO, 0, false, true, null, Map.of()),
            new ErpProductSnapshot("JACKET-001-BLACK-M", "Denim Jacket - Black - M", "Outerwear",
                    new BigDecimal("89.50"), 12, false, false, "JACKET-001",
                    Map.of("Color", "Black", "Size", "M")),
            new ErpProductSnapshot("JACKET-001-BLACK-L", "Denim Jacket - Black - L", "Outerwear",
                    new BigDecimal("89.50"), 8, false, false, "JACKET-001",
                    Map.of("Color", "Black", "Size", "L")),

            // Standalone item (no variants)
            new ErpProductSnapshot("HAT-001", "Summer Hat", "Accessories",
                    new BigDecimal("15.00"), 30, false, false, null, Map.of())
    );

    @Override
    public List<ErpProductSnapshot> fetchPage(int page, int limit) {
        return page == 1 ? PAGE_ONE : List.of();
    }
}
