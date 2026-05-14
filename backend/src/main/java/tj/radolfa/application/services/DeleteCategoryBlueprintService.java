package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.DeleteCategoryBlueprintUseCase;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort;
import tj.radolfa.application.ports.out.SaveCategoryBlueprintPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;

@Service
public class DeleteCategoryBlueprintService implements DeleteCategoryBlueprintUseCase {

    private final LoadCategoryBlueprintPort loadBlueprintPort;
    private final SaveCategoryBlueprintPort saveBlueprintPort;

    public DeleteCategoryBlueprintService(LoadCategoryBlueprintPort loadBlueprintPort,
                                          SaveCategoryBlueprintPort saveBlueprintPort) {
        this.loadBlueprintPort = loadBlueprintPort;
        this.saveBlueprintPort = saveBlueprintPort;
    }

    @Override
    @Transactional
    public void execute(Long categoryId, Long blueprintId) {
        boolean exists = loadBlueprintPort.findByCategoryId(categoryId)
                .stream()
                .anyMatch(e -> e.id().equals(blueprintId));

        if (!exists) {
            throw new ResourceNotFoundException(
                    "Blueprint not found: id=" + blueprintId + " for categoryId=" + categoryId);
        }

        saveBlueprintPort.deleteById(blueprintId);
    }
}
