package tj.radolfa.application.ports.out;

import java.util.Optional;

public interface LoadBrandPort {

    Optional<BrandView> findById(Long id);

    record BrandView(Long id, String name) {}
}
