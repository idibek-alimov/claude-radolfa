package tj.radolfa.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Framework-agnostic pagination wrapper.
 * Pure Java — no Spring, no JPA dependencies.
 *
 * @param content       the items on this page
 * @param totalElements total count across all pages
 * @param number        current page number (1-based)
 * @param size          requested page size
 * @param last          true when no further pages exist
 * @param <T>           the type of items in the page
 */
public record PageResult<T>(
        List<T> content,
        long totalElements,
        int number,
        int size,
        boolean last
) {
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(
                content.stream().map(mapper).toList(),
                totalElements, number, size, last);
    }
}
