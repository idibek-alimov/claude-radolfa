package tj.radolfa.application.ports.in.order;

import java.util.List;

public interface GetPickpointSummariesUseCase {

    record PickpointSummary(
            Long pickpointId,
            String name,
            int incoming,
            int awaitingPickup,
            int overdue,
            int returnInProgress,
            int customerReturns) {}

    List<PickpointSummary> execute();
}
