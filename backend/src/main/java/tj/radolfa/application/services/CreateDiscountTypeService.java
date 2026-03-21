package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.type.CreateDiscountTypeUseCase;
import tj.radolfa.application.ports.out.SaveDiscountTypePort;
import tj.radolfa.domain.model.DiscountType;

@Service
@Transactional
public class CreateDiscountTypeService implements CreateDiscountTypeUseCase {

    private final SaveDiscountTypePort saveDiscountTypePort;

    public CreateDiscountTypeService(SaveDiscountTypePort saveDiscountTypePort) {
        this.saveDiscountTypePort = saveDiscountTypePort;
    }

    @Override
    public DiscountType execute(Command command) {
        return saveDiscountTypePort.save(new DiscountType(null, command.name(), command.rank()));
    }
}
