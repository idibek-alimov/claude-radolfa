package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Warehouse;

import java.util.Optional;

public interface LoadWarehousePort {
    Warehouse findDefault();
    Optional<Warehouse> findById(Long id);
}
