package tj.radolfa.infrastructure.web;

import tj.radolfa.domain.model.PageResult;

import java.util.List;

/**
 * Web-layer pagination wrapper returned by REST controllers.
 *
 * <p>Adds {@code totalPages} and {@code first} (computed from {@link PageResult})
 * so the frontend has the full standard pagination contract.
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(PageResult<T> page) {
        int totalPages = page.size() > 0
                ? (int) Math.ceil((double) page.totalElements() / page.size())
                : 0;
        return new PageResponse<>(
                page.content(),
                page.totalElements(),
                totalPages,
                page.number(),
                page.size(),
                page.number() == 1,
                page.last()
        );
    }
}
