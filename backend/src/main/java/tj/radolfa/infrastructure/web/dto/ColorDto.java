package tj.radolfa.infrastructure.web.dto;

public record ColorDto(
        Long id,
        String colorKey,
        String displayName,
        String hexCode
) {}
