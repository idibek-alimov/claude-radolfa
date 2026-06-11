package tj.radolfa.application.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.event.DiscountTargetsChangedEvent;
import tj.radolfa.application.ports.in.discount.DisableDiscountUseCase;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;

@Service
@Transactional
public class DisableDiscountService implements DisableDiscountUseCase {

    private final LoadDiscountPort loadDiscountPort;
    private final SaveDiscountPort saveDiscountPort;
    private final ApplicationEventPublisher eventPublisher;

    public DisableDiscountService(LoadDiscountPort loadDiscountPort, SaveDiscountPort saveDiscountPort,
                                  ApplicationEventPublisher eventPublisher) {
        this.loadDiscountPort = loadDiscountPort;
        this.saveDiscountPort = saveDiscountPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Discount execute(Command command) {
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
        eventPublisher.publishEvent(new DiscountTargetsChangedEvent(saved.targets()));
        return saved;
    }
}
