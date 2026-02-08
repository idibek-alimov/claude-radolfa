package tj.radolfa.domain.model;

import java.util.List;

/**
 * Framework-agnostic pagination wrapper.
 * Pure Java — no Spring, no JPA dependencies.
 *
 * @param <T> the type of items in the page
 */
public record PageResult<T>(
        List<T> items,
        long totalElements,
        int page,
        boolean hasMore
) {}
