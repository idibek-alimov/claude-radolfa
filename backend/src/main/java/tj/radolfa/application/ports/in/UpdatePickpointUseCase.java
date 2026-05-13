package tj.radolfa.application.ports.in;

import tj.radolfa.application.command.UpdatePickpointCommand;
import tj.radolfa.domain.model.Pickpoint;

public interface UpdatePickpointUseCase {
    Pickpoint execute(UpdatePickpointCommand command);
}
