package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Pickpoint;

public interface CreatePickpointUseCase {
    Pickpoint execute(String name, String address);
}
