package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.HomeBanner;

public interface SaveHomeBannerPort {
    HomeBanner save(HomeBanner banner);
    void delete(Long id);
}
