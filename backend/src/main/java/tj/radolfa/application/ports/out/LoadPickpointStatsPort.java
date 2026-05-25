package tj.radolfa.application.ports.out;

import java.time.Instant;
import java.util.List;

public interface LoadPickpointStatsPort {

    record OrderCountRow(Long pickpointId, long incoming, long awaitingPickup, long overdue, long returnInProgress) {}

    record CustomerReturnCountRow(Long pickpointId, long count) {}

    List<OrderCountRow> countOrdersByPickpointAndStatus(Instant overdueCutoff);

    List<CustomerReturnCountRow> countCustomerReturnsReceived();
}
