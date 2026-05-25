package tj.radolfa.application.ports.out;

import java.time.Instant;
import java.util.Map;

public interface LoadCourierOrderStatsPort {

    /** Per-courier active + delivered-today + attempted counts. Key = courierId. */
    Map<Long, CourierOrderStats> loadStats(Instant todayStart);

    record CourierOrderStats(long deliveredToday, long inTransit, long attempted) {
        public static CourierOrderStats empty() {
            return new CourierOrderStats(0L, 0L, 0L);
        }
    }
}
