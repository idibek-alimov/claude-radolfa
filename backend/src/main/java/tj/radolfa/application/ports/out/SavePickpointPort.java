package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Pickpoint;

public interface SavePickpointPort {
    Pickpoint save(String name, String address);
    Pickpoint update(Long id, String name, String address, boolean active);
}
