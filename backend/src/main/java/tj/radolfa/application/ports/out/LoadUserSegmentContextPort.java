package tj.radolfa.application.ports.out;

import java.util.Optional;

public interface LoadUserSegmentContextPort {

    record UserSegmentContext(Long userId, Long loyaltyTierId, boolean isNewCustomer) {}

    Optional<UserSegmentContext> loadFor(Long userId);
}
