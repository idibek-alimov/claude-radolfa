package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.Seller;

import java.time.Instant;

public record SellerDto(
        Long id,
        Long userId,
        String shopName,
        String logoUrl,
        String bio,
        Instant createdAt
) {
    public static SellerDto from(Seller seller) {
        return new SellerDto(
                seller.id(),
                seller.userId(),
                seller.shopName(),
                seller.logoUrl(),
                seller.bio(),
                seller.createdAt());
    }
}
