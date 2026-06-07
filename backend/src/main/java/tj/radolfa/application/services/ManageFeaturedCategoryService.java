package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.GetCategoryUseCase;
import tj.radolfa.application.ports.in.home.ManageFeaturedCategoryUseCase;
import tj.radolfa.application.ports.out.LoadFeaturedCategoryPort;
import tj.radolfa.application.ports.out.SaveFeaturedCategoryPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.FeaturedCategory;

@Service
@Transactional
public class ManageFeaturedCategoryService implements ManageFeaturedCategoryUseCase {

    private final LoadFeaturedCategoryPort loadFeaturedCategoryPort;
    private final SaveFeaturedCategoryPort saveFeaturedCategoryPort;
    private final GetCategoryUseCase getCategoryUseCase;

    public ManageFeaturedCategoryService(LoadFeaturedCategoryPort loadFeaturedCategoryPort,
                                         SaveFeaturedCategoryPort saveFeaturedCategoryPort,
                                         GetCategoryUseCase getCategoryUseCase) {
        this.loadFeaturedCategoryPort = loadFeaturedCategoryPort;
        this.saveFeaturedCategoryPort = saveFeaturedCategoryPort;
        this.getCategoryUseCase = getCategoryUseCase;
    }

    @Override
    public FeaturedCategory create(Command command) {
        requireCategoryExists(command.categoryId());
        FeaturedCategory featuredCategory = new FeaturedCategory(
                null,
                command.categoryId(),
                command.imageUrl(),
                command.title(),
                command.subtitle(),
                command.displayOrder(),
                command.active());
        return saveFeaturedCategoryPort.save(featuredCategory);
    }

    @Override
    public FeaturedCategory update(Long id, Command command) {
        loadFeaturedCategoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeaturedCategory not found: id=" + id));
        requireCategoryExists(command.categoryId());
        FeaturedCategory updated = new FeaturedCategory(
                id,
                command.categoryId(),
                command.imageUrl(),
                command.title(),
                command.subtitle(),
                command.displayOrder(),
                command.active());
        return saveFeaturedCategoryPort.save(updated);
    }

    @Override
    public void delete(Long id) {
        saveFeaturedCategoryPort.delete(id);
    }

    private void requireCategoryExists(Long categoryId) {
        getCategoryUseCase.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: id=" + categoryId));
    }
}
