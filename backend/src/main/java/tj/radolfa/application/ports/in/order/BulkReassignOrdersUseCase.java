package tj.radolfa.application.ports.in.order;

import java.util.List;

public interface BulkReassignOrdersUseCase {

    record Command(List<Long> orderIds, Long newCourierId) {}

    void execute(Command command);
}
