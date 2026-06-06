package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.home.GetActiveHomeBannersUseCase;
import tj.radolfa.application.ports.out.LoadHomeBannerPort;
import tj.radolfa.domain.model.HomeBanner;

import java.util.List;

@Service
public class GetActiveHomeBannersService implements GetActiveHomeBannersUseCase {

    private final LoadHomeBannerPort loadHomeBannerPort;

    public GetActiveHomeBannersService(LoadHomeBannerPort loadHomeBannerPort) {
        this.loadHomeBannerPort = loadHomeBannerPort;
    }

    @Override
    public List<HomeBanner> execute() {
        return loadHomeBannerPort.findActive();
    }
}
