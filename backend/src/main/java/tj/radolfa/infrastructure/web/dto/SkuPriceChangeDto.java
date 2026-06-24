package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.SkuPriceChange;

import java.math.BigDecimal;
import java.time.Instant;

public record SkuPriceChangeDto(
        Long id, Long skuId, String skuCode,
        BigDecimal oldPrice, BigDecimal newPrice,
        Long actorUserId, String source, Instant occurredAt) {

    public static SkuPriceChangeDto from(SkuPriceChange c) {
        return new SkuPriceChangeDto(c.id(), c.skuId(), c.skuCode(), c.oldPrice(),
                c.newPrice(), c.actorUserId(), c.source(), c.occurredAt());
    }
}
