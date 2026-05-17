package tj.radolfa.domain.model;

/**
 * A single line item within a {@link CustomerReturn}.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson dependencies.
 */
public record CustomerReturnItem(
        Long         id,
        Long         returnId,
        Long         orderItemId,
        int          quantity,
        ReturnReason reason,
        String       notes
) {}
