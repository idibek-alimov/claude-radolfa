package tj.radolfa.infrastructure.web.dto;

import java.util.List;

public record AddressBookDto(
        Long userId,
        List<AddressDto> addresses
) {}
