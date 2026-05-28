package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.ProductStatus;

import java.time.Instant;

public record AdminProductRow(
        Long productBaseId,
        String externalRef,
        String name,
        ProductStatus status,
        String rejectionReason,
        String primaryImageUrl,
        String productCode,
        Instant updatedAt) {}
