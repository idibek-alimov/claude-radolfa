package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.readmodel.AdminProductRow;
import tj.radolfa.domain.model.ProductStatus;

import java.time.Instant;

public record AdminProductRowDto(
        Long productBaseId,
        String externalRef,
        String name,
        ProductStatus status,
        String rejectionReason,
        String primaryImageUrl,
        String productCode,
        Instant updatedAt) {

    public static AdminProductRowDto from(AdminProductRow row) {
        return new AdminProductRowDto(
                row.productBaseId(), row.externalRef(), row.name(),
                row.status(), row.rejectionReason(),
                row.primaryImageUrl(), row.productCode(), row.updatedAt());
    }
}
