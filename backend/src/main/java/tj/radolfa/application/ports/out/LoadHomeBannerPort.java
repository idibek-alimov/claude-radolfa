package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.HomeBanner;

import java.util.List;
import java.util.Optional;

public interface LoadHomeBannerPort {
    List<HomeBanner> findActive();
    Optional<HomeBanner> findById(Long id);
    List<HomeBanner> findAll();
}
