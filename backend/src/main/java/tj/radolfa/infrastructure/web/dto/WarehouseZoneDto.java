package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.WarehouseZone;

public record WarehouseZoneDto(Long id, String code, String label) {

    public static WarehouseZoneDto from(WarehouseZone zone) {
        return new WarehouseZoneDto(zone.id(), zone.code(), zone.label());
    }
}
