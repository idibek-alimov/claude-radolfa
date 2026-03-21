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
        loadDiscountPort.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Discount not found: " + command.id()));

        DiscountType type = loadDiscountTypePort.findById(command.typeId())
                .orElseThrow(() -> new IllegalArgumentException("Discount type not found: " + command.typeId()));

        createDiscountService.validateNoConflict(command.typeId(), command.itemCodes(), command.id());

        Discount updated = new Discount(
                command.id(),
                type,
                List.copyOf(command.itemCodes()),
                command.discountValue(),
                command.validFrom(),
                command.validUpto(),
                false,
                command.title(),
                command.colorHex()
        );
        return saveDiscountPort.save(updated);
    }
}
