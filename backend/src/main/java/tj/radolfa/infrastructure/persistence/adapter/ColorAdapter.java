package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.SaveColorPort;
import tj.radolfa.infrastructure.persistence.entity.ColorEntity;
import tj.radolfa.infrastructure.persistence.repository.ColorRepository;

import java.util.List;
import java.util.Optional;

@Component
public class ColorAdapter implements LoadColorPort, SaveColorPort {

    private final ColorRepository colorRepo;

    public ColorAdapter(ColorRepository colorRepo) {
        this.colorRepo = colorRepo;
    }

    @Override
    public Optional<ColorView> findByColorKey(String colorKey) {
        return colorRepo.findByColorKey(colorKey).map(this::toView);
    }

    @Override
    public List<ColorView> findAll() {
        return colorRepo.findAll().stream().map(this::toView).toList();
    }

    @Override
    public Optional<ColorView> findById(Long id) {
        return colorRepo.findById(id).map(this::toView);
    }

    @Override
    public ColorView save(String colorKey, String displayName, String hexCode) {
        ColorEntity entity = new ColorEntity();
        entity.setColorKey(colorKey);
        entity.setDisplayName(displayName);
        entity.setHexCode(hexCode);
        return toView(colorRepo.save(entity));
    }

    @Override
    public ColorView update(Long id, String displayName, String hexCode) {
        ColorEntity entity = colorRepo.getReferenceById(id);
        if (displayName != null) {
            entity.setDisplayName(displayName);
        }
        if (hexCode != null) {
            entity.setHexCode(hexCode);
        }
        return toView(colorRepo.save(entity));
    }

    private ColorView toView(ColorEntity entity) {
        return new ColorView(
                entity.getId(),
                entity.getColorKey(),
                entity.getDisplayName(),
                entity.getHexCode()
        );
    }
}
