package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import tj.radolfa.application.ports.in.sync.RemoveDiscountUseCase;
import tj.radolfa.application.ports.in.sync.SyncDiscountUseCase;
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
        LOG.info("[DISCOUNT-SYNC] Upsert externalRuleId={}, itemCodes={}, disabled={}",
                command.externalRuleId(), command.itemCodes(), command.disabled());

        Discount existing = loadPort.findByExternalRuleId(command.externalRuleId())
                .orElse(null);

        Discount discount = new Discount(
                existing != null ? existing.id() : null,
                command.externalRuleId(),
                command.itemCodes(),
                command.discountValue(),
                command.validFrom(),
                command.validUpto(),
                command.disabled(),
                command.title(),
                command.colorHex()
        );

        savePort.save(discount);
        LOG.info("[DISCOUNT-SYNC] {} externalRuleId={}",
                existing != null ? "Updated" : "Created", command.externalRuleId());
    }

    @Override
    @Transactional
    public void execute(String externalRuleId) {
        LOG.info("[DISCOUNT-SYNC] Removing externalRuleId={}", externalRuleId);
        deletePort.deleteByExternalRuleId(externalRuleId);
    }
}
