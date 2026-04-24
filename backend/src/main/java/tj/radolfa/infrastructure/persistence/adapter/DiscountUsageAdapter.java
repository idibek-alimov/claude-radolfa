package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.QueryDiscountUsagePort;
import tj.radolfa.infrastructure.persistence.repository.DiscountApplicationRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class DiscountUsageAdapter implements QueryDiscountUsagePort {

    private final DiscountApplicationRepository repository;

    public DiscountUsageAdapter(DiscountApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Map<Long, Long> countByDiscountIds(Collection<Long> discountIds) {
        if (discountIds.isEmpty()) return Map.of();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : repository.countByDiscountIds(discountIds)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> discountIds, Long userId) {
        if (discountIds.isEmpty()) return Map.of();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : repository.countByDiscountIdsForUser(discountIds, userId)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }
}
