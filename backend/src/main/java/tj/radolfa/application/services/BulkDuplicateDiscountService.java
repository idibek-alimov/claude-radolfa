package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.BulkDuplicateDiscountUseCase;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BulkDuplicateDiscountService implements BulkDuplicateDiscountUseCase {

    private final LoadDiscountPort loadDiscountPort;
    private final SaveDiscountPort saveDiscountPort;

    public BulkDuplicateDiscountService(LoadDiscountPort loadDiscountPort,
                                        SaveDiscountPort saveDiscountPort) {
        this.loadDiscountPort = loadDiscountPort;
        this.saveDiscountPort = saveDiscountPort;
    }

    @Override
    public List<Discount> execute(Command command) {
        List<Discount> created = new ArrayList<>();
        for (Long id : command.ids()) {
            loadDiscountPort.findById(id).ifPresent(original -> {
                Discount copy = new Discount(
                        null,
                        original.type(),
                        new ArrayList<>(original.itemCodes()),
                        original.discountValue(),
                        original.validFrom(),
                        original.validUpto(),
                        true,
                        "Copy of " + original.title(),
                        original.colorHex()
                );
                created.add(saveDiscountPort.save(copy));
            });
        }
        return created;
    }
}
