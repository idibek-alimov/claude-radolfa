package tj.radolfa.application.ports.in.home;

import tj.radolfa.domain.model.HomeBanner;

import java.util.List;

public interface GetActiveHomeBannersUseCase {
    List<HomeBanner> execute();
}
