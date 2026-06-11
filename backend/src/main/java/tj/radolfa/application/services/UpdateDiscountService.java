package tj.radolfa.application.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.event.DiscountTargetsChangedEvent;
import tj.radolfa.application.ports.in.discount.UpdateDiscountUseCase;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadDiscountTypePort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountTarget;
import tj.radolfa.domain.model.DiscountType;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class UpdateDiscountService implements UpdateDiscountUseCase {

    private final LoadDiscountTypePort loadDiscountTypePort;
    private final LoadDiscountPort loadDiscountPort;
    private final SaveDiscountPort saveDiscountPort;
    private final CreateDiscountService createDiscountService;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateDiscountService(LoadDiscountTypePort loadDiscountTypePort,
                                 LoadDiscountPort loadDiscountPort,
                                 SaveDiscountPort saveDiscountPort,
                                 CreateDiscountService createDiscountService,
                                 ApplicationEventPublisher eventPublisher) {
        this.loadDiscountTypePort = loadDiscountTypePort;
        this.loadDiscountPort = loadDiscountPort;
        this.saveDiscountPort = saveDiscountPort;
        this.createDiscountService = createDiscountService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Discount execute(Command command) {
        Discount existing = loadDiscountPort.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Discount not found: " + command.id()));

        DiscountType type = loadDiscountTypePort.findById(command.typeId())
                .orElseThrow(() -> new IllegalArgumentException("Discount type not found: " + command.typeId()));

        List<String> skuCodes = CreateDiscountService.extractSkuCodes(command.targets());
        createDiscountService.validateNoConflict(command.typeId(), skuCodes,
                command.validFrom(), command.validUpto(), command.id());

        Discount updated = new Discount(
                command.id(),
                type,
                List.copyOf(command.targets()),
                command.amountType(),
                command.amountValue(),
                command.validFrom(),
                command.validUpto(),
                existing.disabled(),
                command.title(),
                CreateDiscountService.normalizeColorHex(command.colorHex()),
                command.minBasketAmount(),
                command.usageCapTotal(),
                command.usageCapPerCustomer(),
                command.couponCode()
        );
        Discount saved = saveDiscountPort.save(updated);

        // Reindex both the old and new target sets so SKUs/categories removed from the
        // campaign also get recomputed (and drop out of the "Any discount" filter).
        List<DiscountTarget> affected = new ArrayList<>(existing.targets());
        affected.addAll(saved.targets());
        eventPublisher.publishEvent(new DiscountTargetsChangedEvent(affected));

        return saved;
    }
}
