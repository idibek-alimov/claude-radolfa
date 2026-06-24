package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record SkuPriceChange(
        Long id, Long skuId, String skuCode,
        BigDecimal oldPrice,        // null on first-ever price set
        BigDecimal newPrice,
        Long actorUserId, String source,   // "ADMIN_PANEL" | "SELLER_PANEL"
        Instant occurredAt) {}
