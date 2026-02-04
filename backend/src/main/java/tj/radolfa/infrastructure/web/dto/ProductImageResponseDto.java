package tj.radolfa.infrastructure.web.dto;

import java.util.List;

/**
 * Response body returned by the image-upload endpoint.
 *
 * Contains only the fields relevant to the caller: the stable ERP key
 * and the full list of image URLs after the new one has been appended.
 * The internal DB {@code id} is deliberately omitted from the API surface.
 */
public record ProductImageResponseDto(String erpId, List<String> images) {}
