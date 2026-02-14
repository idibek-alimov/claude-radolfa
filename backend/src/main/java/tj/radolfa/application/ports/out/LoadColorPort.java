package tj.radolfa.application.ports.out;

import java.util.List;
import java.util.Optional;

/**
 * Out-Port: read operations for colors.
 */
public interface LoadColorPort {

    record ColorView(Long id, String colorKey, String displayName, String hexCode) {}

    Optional<ColorView> findByColorKey(String colorKey);

    List<ColorView> findAll();

    Optional<ColorView> findById(Long id);
}
