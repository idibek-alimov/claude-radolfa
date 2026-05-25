package tj.radolfa.domain.model;

import java.time.Instant;

public record Warehouse(Long id, String code, String name, boolean isDefault, Instant createdAt) {}
