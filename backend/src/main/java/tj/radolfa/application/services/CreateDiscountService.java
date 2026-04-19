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
import tj.radolfa.domain.model.DiscountTarget;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.SkuTarget;

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

        List<String> skuCodes = extractSkuCodes(command.targets());
        validateNoConflict(command.typeId(), skuCodes, command.validFrom(), command.validUpto(), null);

        Discount discount = new Discount(
                null,
                type,
                List.copyOf(command.targets()),
                command.amountType(),
                command.amountValue(),
                command.validFrom(),
                command.validUpto(),
                false,
                command.title(),
                normalizeColorHex(command.colorHex()),
                command.minBasketAmount(),
                command.usageCapTotal(),
                command.usageCapPerCustomer(),
                command.couponCode()
        );
        return saveDiscountPort.save(discount);
    }

    static String normalizeColorHex(String hex) {
        return hex != null && hex.startsWith("#") ? hex.substring(1) : hex;
    }

    static List<String> extractSkuCodes(List<DiscountTarget> targets) {
        return targets.stream()
                .filter(t -> t instanceof SkuTarget)
                .map(t -> ((SkuTarget) t).itemCode())
                .toList();
    }

    void validateNoConflict(Long typeId, List<String> skuCodes,
                            Instant validFrom, Instant validUpto, Long excludeId) {
        if (skuCodes.isEmpty()) return;
        DiscountFilter filter = new DiscountFilter(typeId, null, null, null, null);
        loadDiscountPort.findAll(filter, Pageable.unpaged())
                .forEach(existing -> {
                    if (excludeId != null && excludeId.equals(existing.id())) return;
                    if (existing.disabled()) return;
                    boolean datesOverlap = !existing.validUpto().isBefore(validFrom)
                            && !existing.validFrom().isAfter(validUpto);
                    if (!datesOverlap) return;
                    boolean coversItem = existing.itemCodes().stream().anyMatch(skuCodes::contains);
                    if (coversItem) {
                        throw new IllegalStateException(
                                "A discount of the same type already covers one or more of the target SKUs " +
                                "in an overlapping date range. Resolve the conflict before creating a new " +
                                "discount (existing id=" + existing.id() + ")");
                    }
                });
    }
}
