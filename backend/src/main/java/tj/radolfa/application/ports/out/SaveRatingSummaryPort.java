package tj.radolfa.application.ports.out;

import java.math.BigDecimal;
import java.util.Map;

public interface SaveRatingSummaryPort {

    void upsert(Long listingVariantId,
                BigDecimal averageRating,
                int reviewCount,
                Map<Integer, Integer> distribution,
                int sizeAccurate,
                int sizeRunsSmall,
                int sizeRunsLarge);
}
