package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.discount.RecordDiscountApplicationUseCase;
import tj.radolfa.application.ports.out.SaveDiscountApplicationPort;
import tj.radolfa.domain.model.DiscountApplication;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class RecordDiscountApplicationService implements RecordDiscountApplicationUseCase {

    private final SaveDiscountApplicationPort savePort;

    public RecordDiscountApplicationService(SaveDiscountApplicationPort savePort) {
        this.savePort = savePort;
    }

    @Override
    public void execute(Command cmd) {
        BigDecimal perUnitDelta = cmd.originalUnitPrice().subtract(cmd.appliedUnitPrice());
        BigDecimal totalDelta   = perUnitDelta.multiply(BigDecimal.valueOf(cmd.quantity()));
        DiscountApplication app = new DiscountApplication(
                null,
                cmd.discountId(),
                cmd.orderId(),
                cmd.orderLineId(),
                cmd.skuItemCode(),
                cmd.quantity(),
                cmd.originalUnitPrice(),
                cmd.appliedUnitPrice(),
                totalDelta,
                Instant.now()
        );
        savePort.save(app);
    }
}
