package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.PlacementView;

public record PlacementDto(String binLabel, int quantity) {

    public static PlacementDto from(PlacementView v) {
        return new PlacementDto(v.binLabel(), v.quantity());
    }
}
