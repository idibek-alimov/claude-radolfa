package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Pickpoint;

import java.util.List;

public interface ListAllPickpointsUseCase {
    List<Pickpoint> execute();
}
