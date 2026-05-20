package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.WarehouseBin;

public record WarehouseBinDto(Long id, Long shelfId, String code) {

    public static WarehouseBinDto from(WarehouseBin bin) {
        return new WarehouseBinDto(bin.id(), bin.shelfId(), bin.code());
    }
}
