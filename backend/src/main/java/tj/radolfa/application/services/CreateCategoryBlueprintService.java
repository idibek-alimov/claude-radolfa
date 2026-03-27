package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.CreateCategoryBlueprintUseCase;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.application.ports.out.SaveCategoryBlueprintPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;

@Service
public class CreateCategoryBlueprintService implements CreateCategoryBlueprintUseCase {

    private final LoadCategoryPort loadCategoryPort;
    private final SaveCategoryBlueprintPort saveBlueprintPort;

    public CreateCategoryBlueprintService(LoadCategoryPort loadCategoryPort,
                                          SaveCategoryBlueprintPort saveBlueprintPort) {
        this.loadCategoryPort = loadCategoryPort;
        this.saveBlueprintPort = saveBlueprintPort;
    }

    @Override
    @Transactional
    public Long execute(Command command) {
        loadCategoryPort.findById(command.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: id=" + command.categoryId()));

        return saveBlueprintPort.save(
                command.categoryId(),
                command.attributeKey(),
                command.type(),
                command.unitName(),
                command.allowedValues(),
                command.required(),
                command.sortOrder());
    }
}
