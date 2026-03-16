package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadColorImagesPort;
import tj.radolfa.application.ports.out.SaveColorImagesPort;
import tj.radolfa.domain.model.ColorImages;
import tj.radolfa.infrastructure.persistence.entity.ProductColorImagesEntity;
import tj.radolfa.infrastructure.persistence.repository.ProductColorImagesRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductTemplateRepository;

import java.util.List;
import java.util.Optional;

@Component
public class ColorImagesPersistenceAdapter implements LoadColorImagesPort, SaveColorImagesPort {

    private final ProductColorImagesRepository repo;
    private final ProductTemplateRepository templateRepo;

    public ColorImagesPersistenceAdapter(ProductColorImagesRepository repo,
                                         ProductTemplateRepository templateRepo) {
        this.repo = repo;
        this.templateRepo = templateRepo;
    }

    @Override
    public Optional<ColorImages> findByTemplateAndColor(Long templateId, String colorKey) {
        return repo.findByTemplateIdAndColorKey(templateId, colorKey)
                .map(this::toDomain);
    }

    @Override
    public List<String> findImagesByTemplateAndColor(Long templateId, String colorKey) {
        return repo.findByTemplateIdAndColorKey(templateId, colorKey)
                .map(ProductColorImagesEntity::getImages)
                .orElse(List.of());
    }

    @Override
    public ColorImages save(ColorImages colorImages) {
        ProductColorImagesEntity entity;

        if (colorImages.getId() != null) {
            entity = repo.findById(colorImages.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "ColorImages not found: " + colorImages.getId()));
            entity.setImages(List.copyOf(colorImages.getImages()));
        } else {
            entity = new ProductColorImagesEntity();
            entity.setTemplate(templateRepo.getReferenceById(colorImages.getTemplateId()));
            entity.setColorKey(colorImages.getColorKey());
            entity.setImages(List.copyOf(colorImages.getImages()));
        }

        ProductColorImagesEntity saved = repo.save(entity);
        return toDomain(saved);
    }

    private ColorImages toDomain(ProductColorImagesEntity entity) {
        return new ColorImages(
                entity.getId(),
                entity.getTemplate().getId(),
                entity.getColorKey(),
                entity.getImages()
        );
    }
}
