package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.home.ManageHomeBannerUseCase;
import tj.radolfa.application.ports.out.LoadHomeBannerPort;
import tj.radolfa.application.ports.out.SaveHomeBannerPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.HomeBanner;

@Service
@Transactional
public class ManageHomeBannerService implements ManageHomeBannerUseCase {

    private final LoadHomeBannerPort loadHomeBannerPort;
    private final SaveHomeBannerPort saveHomeBannerPort;

    public ManageHomeBannerService(LoadHomeBannerPort loadHomeBannerPort,
                                   SaveHomeBannerPort saveHomeBannerPort) {
        this.loadHomeBannerPort = loadHomeBannerPort;
        this.saveHomeBannerPort = saveHomeBannerPort;
    }

    @Override
    public HomeBanner create(Command command) {
        HomeBanner banner = new HomeBanner(
                null,
                command.slot(),
                command.title(),
                command.subtitle(),
                command.badgeText(),
                command.ctaLabel(),
                command.ctaUrl(),
                command.bgColorHex(),
                command.expiresAt(),
                command.active());
        return saveHomeBannerPort.save(banner);
    }

    @Override
    public HomeBanner update(Long id, Command command) {
        loadHomeBannerPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HomeBanner not found: id=" + id));
        HomeBanner updated = new HomeBanner(
                id,
                command.slot(),
                command.title(),
                command.subtitle(),
                command.badgeText(),
                command.ctaLabel(),
                command.ctaUrl(),
                command.bgColorHex(),
                command.expiresAt(),
                command.active());
        return saveHomeBannerPort.save(updated);
    }

    @Override
    public void delete(Long id) {
        saveHomeBannerPort.delete(id);
    }
}
