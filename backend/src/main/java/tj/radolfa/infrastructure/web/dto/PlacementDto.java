package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.PlacementView;

public record PlacementDto(Long binId, String binLabel, int quantity) {

    public static PlacementDto from(PlacementView v) {
        return new PlacementDto(v.binId(), v.binLabel(), v.quantity());
    }
}
