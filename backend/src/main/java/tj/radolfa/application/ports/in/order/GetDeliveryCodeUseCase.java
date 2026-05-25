package tj.radolfa.application.ports.in.order;

import java.time.Instant;

public interface GetDeliveryCodeUseCase {

    record Result(String code, Instant expiresAt) {}

    Result execute(Long orderId, Long requestingUserId);
}
