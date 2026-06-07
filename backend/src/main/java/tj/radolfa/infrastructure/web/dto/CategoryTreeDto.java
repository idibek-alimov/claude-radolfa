package tj.radolfa.infrastructure.web.dto;

import java.util.List;

public record CategoryTreeDto(
        Long id,
        String name,
        String slug,
        List<CategoryTreeDto> children
) {}
