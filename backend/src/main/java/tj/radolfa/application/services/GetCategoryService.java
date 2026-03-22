package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.GetCategoryUseCase;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.readmodel.CategoryView;

import java.util.List;
import java.util.Optional;

@Service
public class GetCategoryService implements GetCategoryUseCase {

    private final LoadCategoryPort loadCategoryPort;

    public GetCategoryService(LoadCategoryPort loadCategoryPort) {
        this.loadCategoryPort = loadCategoryPort;
    }

    @Override
    public List<CategoryView> findAll() {
        return loadCategoryPort.findAll();
    }

    @Override
    public Optional<CategoryView> findBySlug(String slug) {
        return loadCategoryPort.findBySlug(slug);
    }

    @Override
    public List<Long> getDescendantIds(Long categoryId) {
        return loadCategoryPort.getAllDescendantIds(categoryId);
    }
}
