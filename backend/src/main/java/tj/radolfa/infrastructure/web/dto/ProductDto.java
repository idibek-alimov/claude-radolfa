package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Product response DTO for the REST API.
 *
 * All fields are visible to clients. ERP-locked fields (name, price, stock)
 * are read-only from the web application's perspective.
 */
public record ProductDto(
        Long id,
        String erpId,
        String name,
        BigDecimal price,
        Integer stock,
        String webDescription,
        boolean topSelling,
        List<String> images,
        Instant lastErpSyncAt
) {}
