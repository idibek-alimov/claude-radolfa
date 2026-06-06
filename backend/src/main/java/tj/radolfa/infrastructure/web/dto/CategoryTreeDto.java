package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CategoryTreeDto(
        Long id,
        String name,
        String slug,
        Long productCount,
        BigDecimal minPrice,
        List<CategoryTreeDto> children
) {}
