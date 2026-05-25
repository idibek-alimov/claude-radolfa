package tj.radolfa.application.ports.in.order;

import java.util.List;

public interface GetCourierFleetSummaryUseCase {
    List<CourierFleetEntry> execute();
}
