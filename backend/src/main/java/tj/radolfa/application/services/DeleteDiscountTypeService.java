package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.type.DeleteDiscountTypeUseCase;
import tj.radolfa.application.ports.out.LoadDiscountTypePort;
import tj.radolfa.application.ports.out.SaveDiscountTypePort;
import tj.radolfa.domain.exception.DiscountTypeInUseException;

@Service
@Transactional
public class DeleteDiscountTypeService implements DeleteDiscountTypeUseCase {

    private final LoadDiscountTypePort loadDiscountTypePort;
    private final SaveDiscountTypePort saveDiscountTypePort;

    public DeleteDiscountTypeService(LoadDiscountTypePort loadDiscountTypePort,
                                     SaveDiscountTypePort saveDiscountTypePort) {
        this.loadDiscountTypePort = loadDiscountTypePort;
        this.saveDiscountTypePort = saveDiscountTypePort;
    }

    @Override
    public void execute(Long id) {
        loadDiscountTypePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discount type not found: " + id));

        long count = loadDiscountTypePort.countDiscountsByTypeId(id);
        if (count > 0) {
            throw new DiscountTypeInUseException(id, count);
        }
        saveDiscountTypePort.deleteById(id);
    }
}
