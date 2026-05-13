package tj.radolfa.application.ports.out;

import tj.radolfa.application.command.CreatePickpointCommand;
import tj.radolfa.application.command.UpdatePickpointCommand;
import tj.radolfa.domain.model.Pickpoint;

public interface SavePickpointPort {
    Pickpoint save(CreatePickpointCommand command);
    Pickpoint update(UpdatePickpointCommand command);
}
