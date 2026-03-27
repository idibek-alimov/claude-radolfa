package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.type.UpdateDiscountTypeUseCase;
import tj.radolfa.application.ports.out.LoadDiscountTypePort;
import tj.radolfa.application.ports.out.SaveDiscountTypePort;
import tj.radolfa.domain.model.DiscountType;

@Service
@Transactional
public class UpdateDiscountTypeService implements UpdateDiscountTypeUseCase {

    private final LoadDiscountTypePort loadDiscountTypePort;
    private final SaveDiscountTypePort saveDiscountTypePort;

    public UpdateDiscountTypeService(LoadDiscountTypePort loadDiscountTypePort,
                                     SaveDiscountTypePort saveDiscountTypePort) {
        this.loadDiscountTypePort = loadDiscountTypePort;
        this.saveDiscountTypePort = saveDiscountTypePort;
    }

    @Override
    public DiscountType execute(Command command) {
        loadDiscountTypePort.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Discount type not found: " + command.id()));

        return saveDiscountTypePort.save(new DiscountType(command.id(), command.name(), command.rank()));
    }
}
