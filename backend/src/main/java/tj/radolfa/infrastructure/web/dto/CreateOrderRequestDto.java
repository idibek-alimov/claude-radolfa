package tj.radolfa.infrastructure.web.dto;

import java.util.Map;

public record CreateOrderRequestDto(
        Map<String, Integer> items) {
}
