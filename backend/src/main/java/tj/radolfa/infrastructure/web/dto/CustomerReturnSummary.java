package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.Money;

import java.time.Instant;

public record CustomerReturnSummary(
        Long returnId,
        CustomerReturnStatus status,
        Instant receivedAt,
        int itemCount,
        Money totalRefundAmount) {}
