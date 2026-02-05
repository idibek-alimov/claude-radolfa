package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductRequestDto(
        String name,
        BigDecimal price,
        Integer stock,
        String webDescription,
        boolean topSelling,
        List<String> images) {
}
