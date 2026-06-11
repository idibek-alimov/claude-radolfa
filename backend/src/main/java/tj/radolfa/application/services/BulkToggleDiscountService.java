package tj.radolfa.application.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.event.DiscountTargetsChangedEvent;
import tj.radolfa.application.ports.in.discount.BulkToggleDiscountUseCase;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountTarget;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BulkToggleDiscountService implements BulkToggleDiscountUseCase {

    private final LoadDiscountPort loadDiscountPort;
    private final SaveDiscountPort saveDiscountPort;
    private final ApplicationEventPublisher eventPublisher;

    public BulkToggleDiscountService(LoadDiscountPort loadDiscountPort,
                                     SaveDiscountPort saveDiscountPort,
                                     ApplicationEventPublisher eventPublisher) {
        this.loadDiscountPort = loadDiscountPort;
        this.saveDiscountPort = saveDiscountPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public int execute(Command command) {
        int affected = 0;
        List<DiscountTarget> affectedTargets = new ArrayList<>();
        for (Long id : command.ids()) {
            var opt = loadDiscountPort.findById(id);
            if (opt.isEmpty()) continue;
            Discount original = opt.get();
            Discount updated = new Discount(
                    original.id(), original.type(), original.targets(),
                    original.amountType(), original.amountValue(),
                    original.validFrom(), original.validUpto(),
                    command.disabled(), original.title(), original.colorHex(),
                    original.minBasketAmount(), original.usageCapTotal(),
                    original.usageCapPerCustomer(), original.couponCode()
            );
            saveDiscountPort.save(updated);
            affectedTargets.addAll(original.targets());
            affected++;
        }
        if (!affectedTargets.isEmpty()) {
            eventPublisher.publishEvent(new DiscountTargetsChangedEvent(affectedTargets));
        }
        return affected;
    }
}
