package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ColorImages;

import java.util.List;
import java.util.Optional;

public interface LoadColorImagesPort {

    Optional<ColorImages> findByTemplateAndColor(Long templateId, String colorKey);

    List<String> findImagesByTemplateAndColor(Long templateId, String colorKey);
}
