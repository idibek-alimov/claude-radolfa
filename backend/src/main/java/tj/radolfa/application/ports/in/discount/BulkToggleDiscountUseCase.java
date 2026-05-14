package tj.radolfa.application.ports.in.discount;

import java.util.List;

public interface BulkToggleDiscountUseCase {
    record Command(List<Long> ids, boolean disabled) {}
    int execute(Command command);
}
