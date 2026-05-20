package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.WarehouseShelf;

public record WarehouseShelfDto(Long id, Long zoneId, String code, String label) {

    public static WarehouseShelfDto from(WarehouseShelf shelf) {
        return new WarehouseShelfDto(shelf.id(), shelf.zoneId(), shelf.code(), shelf.label());
    }
}
