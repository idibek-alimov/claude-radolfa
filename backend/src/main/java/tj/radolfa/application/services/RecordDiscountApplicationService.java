package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.RecordDiscountApplicationUseCase;
import tj.radolfa.application.ports.out.LockDiscountForUsagePort;
import tj.radolfa.application.ports.out.QueryDiscountUsagePort;
import tj.radolfa.application.ports.out.SaveDiscountApplicationPort;
import tj.radolfa.domain.exception.DiscountUsageCapExceededException;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountApplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class RecordDiscountApplicationService implements RecordDiscountApplicationUseCase {

    private final LockDiscountForUsagePort lockDiscountPort;
    private final QueryDiscountUsagePort queryUsagePort;
    private final SaveDiscountApplicationPort savePort;

    public RecordDiscountApplicationService(LockDiscountForUsagePort lockDiscountPort,
                                            QueryDiscountUsagePort queryUsagePort,
                                            SaveDiscountApplicationPort savePort) {
        this.lockDiscountPort = lockDiscountPort;
        this.queryUsagePort   = queryUsagePort;
        this.savePort         = savePort;
    }

    @Override
    @Transactional
    public void execute(Command cmd) {
        Discount discount = lockDiscountPort.lockById(cmd.discountId())
                .orElseThrow(() -> new IllegalStateException("Discount not found: " + cmd.discountId()));

        enforceCaps(discount, cmd.userId());

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

    private void enforceCaps(Discount discount, Long userId) {
        if (discount.usageCapTotal() != null) {
            Map<Long, Long> totals = queryUsagePort.countByDiscountIds(List.of(discount.id()));
            long used = totals.getOrDefault(discount.id(), 0L);
            if (used >= discount.usageCapTotal()) {
                throw new DiscountUsageCapExceededException(discount.id(), DiscountUsageCapExceededException.Scope.TOTAL);
            }
        }
        if (discount.usageCapPerCustomer() != null && userId != null) {
            Map<Long, Long> perUser = queryUsagePort.countByDiscountIdsForUser(List.of(discount.id()), userId);
            long used = perUser.getOrDefault(discount.id(), 0L);
            if (used >= discount.usageCapPerCustomer()) {
                throw new DiscountUsageCapExceededException(discount.id(), DiscountUsageCapExceededException.Scope.PER_CUSTOMER);
            }
        }
    }
}
