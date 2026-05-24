package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.ports.in.warehouse.ScanOrderItemUnitUseCase;

public record ScanResultDto(
        Long orderItemId,
        int quantityPicked,
        int quantityOrdered,
        boolean orderFullyPicked
) {
    public static ScanResultDto from(ScanOrderItemUnitUseCase.Result result) {
        return new ScanResultDto(
                result.orderItemId(),
                result.quantityPicked(),
                result.quantityOrdered(),
                result.orderFullyPicked());
    }
}
