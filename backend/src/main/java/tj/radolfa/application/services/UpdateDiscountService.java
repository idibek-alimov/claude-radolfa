package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.UpdateDiscountUseCase;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadDiscountTypePort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;

import java.util.List;

@Service
@Transactional
public class UpdateDiscountService implements UpdateDiscountUseCase {

    private final LoadDiscountTypePort loadDiscountTypePort;
    private final LoadDiscountPort loadDiscountPort;
    private final SaveDiscountPort saveDiscountPort;
    private final CreateDiscountService createDiscountService;

    public UpdateDiscountService(LoadDiscountTypePort loadDiscountTypePort,
                                 LoadDiscountPort loadDiscountPort,
                                 SaveDiscountPort saveDiscountPort,
                                 CreateDiscountService createDiscountService) {
        this.loadDiscountTypePort = loadDiscountTypePort;
        this.loadDiscountPort = loadDiscountPort;
        this.saveDiscountPort = saveDiscountPort;
        this.createDiscountService = createDiscountService;
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
        return saveDiscountPort.save(updated);
    }
}
