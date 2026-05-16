package tj.radolfa.application.ports.in.product;

import java.math.BigDecimal;

public interface UpdateSkuDimensionsUseCase {

    record Command(Long skuId, BigDecimal weightKg, Integer lengthCm, Integer widthCm, Integer heightCm) {}

    void execute(Command command);
}
