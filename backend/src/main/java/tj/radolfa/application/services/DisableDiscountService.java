package tj.radolfa.application.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.event.DiscountTargetsChangedEvent;
import tj.radolfa.application.ports.in.discount.DisableDiscountUseCase;
import tj.radolfa.application.ports.out.DiscountSnapshotPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.SaveDiscountChangePort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.ChangeType;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountChange;

import java.time.Instant;

@Service
@Transactional
public class DisableDiscountService implements DisableDiscountUseCase {

    private final LoadDiscountPort loadDiscountPort;
    private final SaveDiscountPort saveDiscountPort;
    private final ApplicationEventPublisher eventPublisher;
    private final SaveDiscountChangePort saveDiscountChangePort;
    private final DiscountSnapshotPort discountSnapshotPort;

    public DisableDiscountService(LoadDiscountPort loadDiscountPort, SaveDiscountPort saveDiscountPort,
                                  ApplicationEventPublisher eventPublisher,
                                  SaveDiscountChangePort saveDiscountChangePort,
                                  DiscountSnapshotPort discountSnapshotPort) {
        this.loadDiscountPort = loadDiscountPort;
        this.saveDiscountPort = saveDiscountPort;
        this.eventPublisher = eventPublisher;
        this.saveDiscountChangePort = saveDiscountChangePort;
        this.discountSnapshotPort = discountSnapshotPort;
    }

    @Override
    public Discount execute(Command command, Long actorUserId) {
        Discount existing = loadDiscountPort.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Discount not found: " + command.id()));

        Discount updated = new Discount(
                existing.id(),
                existing.type(),
                existing.targets(),
                existing.amountType(),
                existing.amountValue(),
                existing.validFrom(),
                existing.validUpto(),
                command.disable(),
                existing.title(),
                existing.colorHex(),
                existing.minBasketAmount(),
                existing.usageCapTotal(),
                existing.usageCapPerCustomer(),
                existing.couponCode()
        );
        Discount saved = saveDiscountPort.save(updated);
        saveDiscountChangePort.save(new DiscountChange(null, saved.id(), ChangeType.UPDATE,
                discountSnapshotPort.toJson(existing), discountSnapshotPort.toJson(saved),
                actorUserId, Instant.now()));
        eventPublisher.publishEvent(new DiscountTargetsChangedEvent(saved.targets()));
        return saved;
    }
}
