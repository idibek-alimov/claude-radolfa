package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.application.readmodel.PickSession;

public interface GetPickSessionUseCase {
    PickSession execute(Long orderId);
}
