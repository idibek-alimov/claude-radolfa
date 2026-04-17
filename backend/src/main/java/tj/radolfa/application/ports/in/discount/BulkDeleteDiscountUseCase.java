package tj.radolfa.application.ports.in.discount;

import java.util.List;

public interface BulkDeleteDiscountUseCase {
    record Command(List<Long> ids) {}
    int execute(Command command);
}
