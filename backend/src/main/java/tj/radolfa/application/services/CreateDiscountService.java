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

import java.time.Instant;
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

        validateNoConflict(command.typeId(), command.itemCodes(), command.validFrom(), command.validUpto(), null);

        Discount discount = new Discount(
                null,
                type,
                List.copyOf(command.itemCodes()),
                command.discountValue(),
                command.validFrom(),
                command.validUpto(),
                false,
                command.title(),
                normalizeColorHex(command.colorHex())
        );
        return saveDiscountPort.save(discount);
    }

    /**
     * Rejects if another non-disabled discount of the same type already covers any of the
     * target item codes with an overlapping date range.
     *
     * @param excludeId the discount being updated (null on create)
     */
    static String normalizeColorHex(String hex) {
        return hex != null && hex.startsWith("#") ? hex.substring(1) : hex;
    }

    void validateNoConflict(Long typeId, List<String> itemCodes,
                            Instant validFrom, Instant validUpto, Long excludeId) {
        DiscountFilter filter = new DiscountFilter(typeId, null, null, null);
        loadDiscountPort.findAll(filter, Pageable.unpaged())
                .forEach(existing -> {
                    if (excludeId != null && excludeId.equals(existing.id())) return;
                    if (existing.disabled()) return; // disabled discounts do not block new ones
                    boolean datesOverlap = !existing.validUpto().isBefore(validFrom)
                            && !existing.validFrom().isAfter(validUpto);
                    if (!datesOverlap) return; // non-overlapping date ranges are always allowed
                    boolean coversItem = existing.itemCodes().stream().anyMatch(itemCodes::contains);
                    if (coversItem) {
                        throw new IllegalStateException(
                                "A discount of the same type already covers one or more of the target SKUs " +
                                "in an overlapping date range. Resolve the conflict before creating a new " +
                                "discount (existing id=" + existing.id() + ")");
                    }
                });
    }
}
