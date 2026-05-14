package tj.radolfa.application.ports.in;

import tj.radolfa.application.command.CreatePickpointCommand;
import tj.radolfa.domain.model.Pickpoint;

public interface CreatePickpointUseCase {
    Pickpoint execute(CreatePickpointCommand command);
}
