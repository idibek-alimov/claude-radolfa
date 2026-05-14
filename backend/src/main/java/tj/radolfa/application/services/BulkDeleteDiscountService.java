package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.BulkDeleteDiscountUseCase;
import tj.radolfa.application.ports.out.SaveDiscountPort;

@Service
@Transactional
public class BulkDeleteDiscountService implements BulkDeleteDiscountUseCase {

    private final SaveDiscountPort saveDiscountPort;

    public BulkDeleteDiscountService(SaveDiscountPort saveDiscountPort) {
        this.saveDiscountPort = saveDiscountPort;
    }

    @Override
    public int execute(Command command) {
        for (Long id : command.ids()) {
            saveDiscountPort.delete(id);
        }
        return command.ids().size();
    }
}
