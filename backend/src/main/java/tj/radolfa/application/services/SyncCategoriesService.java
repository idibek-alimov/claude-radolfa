package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tj.radolfa.application.ports.in.SyncCategoriesUseCase;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.LoadCategoryPort.CategoryView;
import tj.radolfa.application.ports.out.SaveCategoryPort;

import java.util.ArrayList;
import java.util.List;

@Service
public class SyncCategoriesService implements SyncCategoriesUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncCategoriesService.class);

    private final LoadCategoryPort loadCategoryPort;
    private final SaveCategoryPort saveCategoryPort;

    public SyncCategoriesService(LoadCategoryPort loadCategoryPort,
                                  SaveCategoryPort saveCategoryPort) {
        this.loadCategoryPort = loadCategoryPort;
        this.saveCategoryPort = saveCategoryPort;
    }

    @Override
    @Transactional
    public List<CategoryView> execute(SyncCategoriesCommand command) {
        LOG.info("[CATEGORY-SYNC] Processing {} categories", command.categories().size());

        List<CategoryView> results = new ArrayList<>();

        for (SyncCategoriesCommand.CategoryPayload payload : command.categories()) {
            CategoryView existing = loadCategoryPort.findByName(payload.name()).orElse(null);

            if (existing != null) {
                results.add(existing);
                continue;
            }

            Long parentId = null;
            if (payload.parentName() != null) {
                parentId = loadCategoryPort.findByName(payload.parentName())
                        .map(CategoryView::id)
                        .orElse(null);
                if (parentId == null) {
                    LOG.warn("[CATEGORY-SYNC] Parent '{}' not found for category '{}', creating as root",
                            payload.parentName(), payload.name());
                }
            }

            String slug = slugify(payload.name());
            CategoryView saved = saveCategoryPort.save(payload.name(), slug, parentId);
            results.add(saved);

            LOG.debug("[CATEGORY-SYNC] Created category: name={}, slug={}, parentId={}",
                    payload.name(), slug, parentId);
        }

        LOG.info("[CATEGORY-SYNC] Completed — synced {} categories", results.size());
        return results;
    }

    private String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
