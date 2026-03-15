package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import tj.radolfa.application.ports.in.RemoveDiscountUseCase;
import tj.radolfa.application.ports.in.SyncDiscountUseCase;
import tj.radolfa.application.ports.out.DeleteDiscountPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;

@Service
public class SyncDiscountService implements SyncDiscountUseCase, RemoveDiscountUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncDiscountService.class);

    private final LoadDiscountPort loadPort;
    private final SaveDiscountPort savePort;
    private final DeleteDiscountPort deletePort;

    public SyncDiscountService(LoadDiscountPort loadPort,
                               SaveDiscountPort savePort,
                               DeleteDiscountPort deletePort) {
        this.loadPort = loadPort;
        this.savePort = savePort;
        this.deletePort = deletePort;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void execute(SyncDiscountCommand command) {
        LOG.info("[DISCOUNT-SYNC] Upsert erpPricingRuleId={}, itemCode={}, disabled={}",
                command.erpPricingRuleId(), command.itemCode(), command.disabled());

        Discount existing = loadPort.findByErpPricingRuleId(command.erpPricingRuleId())
                .orElse(null);

        Discount discount = new Discount(
                existing != null ? existing.id() : null,
                command.erpPricingRuleId(),
                command.itemCode(),
                command.discountValue(),
                command.validFrom(),
                command.validUpto(),
                command.disabled()
        );

        savePort.save(discount);
        LOG.info("[DISCOUNT-SYNC] {} erpPricingRuleId={}",
                existing != null ? "Updated" : "Created", command.erpPricingRuleId());
    }

    @Override
    @Transactional
    public void execute(String erpPricingRuleId) {
        LOG.info("[DISCOUNT-SYNC] Removing erpPricingRuleId={}", erpPricingRuleId);
        deletePort.deleteByErpPricingRuleId(erpPricingRuleId);
    }
}
