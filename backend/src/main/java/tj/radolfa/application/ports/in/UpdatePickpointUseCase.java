package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Pickpoint;

public interface UpdatePickpointUseCase {
    Pickpoint execute(Long id, String name, String address, boolean active);
}
