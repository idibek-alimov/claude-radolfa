package tj.radolfa.application.services;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.CreateDiscountUseCase;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadDiscountTypePort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;

import java.util.List;

@Service
@Transactional
public class CreateDiscountService implements CreateDiscountUseCase {

    private final LoadDiscountTypePort loadDiscountTypePort;
    private final LoadDiscountPort loadDiscountPort;
    private final SaveDiscountPort saveDiscountPort;

    public CreateDiscountService(LoadDiscountTypePort loadDiscountTypePort,
                                 LoadDiscountPort loadDiscountPort,
                                 SaveDiscountPort saveDiscountPort) {
        this.loadDiscountTypePort = loadDiscountTypePort;
        this.loadDiscountPort = loadDiscountPort;
        this.saveDiscountPort = saveDiscountPort;
    }

    @Override
    public Discount execute(Command command) {
        DiscountType type = loadDiscountTypePort.findById(command.typeId())
                .orElseThrow(() -> new IllegalArgumentException("Discount type not found: " + command.typeId()));

        validateNoConflict(command.typeId(), command.itemCodes(), null);

        Discount discount = new Discount(
                null,
                type,
                List.copyOf(command.itemCodes()),
                command.discountValue(),
                command.validFrom(),
                command.validUpto(),
                false,
                command.title(),
                command.colorHex()
        );
        return saveDiscountPort.save(discount);
    }

    /**
     * Rejects if another non-disabled discount of the same type already covers any of the
     * target item codes with an overlapping date range.
     *
     * @param excludeId the discount being updated (null on create)
     */
    void validateNoConflict(Long typeId, List<String> itemCodes, Long excludeId) {
        DiscountFilter filter = new DiscountFilter(typeId, null, null, null);
        loadDiscountPort.findAll(filter, Pageable.unpaged())
                .forEach(existing -> {
                    if (excludeId != null && excludeId.equals(existing.id())) return;
                    boolean coversItem = existing.itemCodes().stream().anyMatch(itemCodes::contains);
                    if (coversItem) {
                        throw new IllegalStateException(
                                "A discount of the same type already covers one or more of the target SKUs. " +
                                "Resolve the conflict before creating a new discount (existing id=" + existing.id() + ")");
                    }
                });
    }
}
