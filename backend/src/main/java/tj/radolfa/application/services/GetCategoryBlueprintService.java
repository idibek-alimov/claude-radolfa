package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.GetCategoryBlueprintUseCase;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort;
import tj.radolfa.application.ports.out.LoadCategoryPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;

import java.util.List;

@Service
public class GetCategoryBlueprintService implements GetCategoryBlueprintUseCase {

    private final LoadCategoryPort loadCategoryPort;
    private final LoadCategoryBlueprintPort loadBlueprintPort;

    public GetCategoryBlueprintService(LoadCategoryPort loadCategoryPort,
                                       LoadCategoryBlueprintPort loadBlueprintPort) {
        this.loadCategoryPort = loadCategoryPort;
        this.loadBlueprintPort = loadBlueprintPort;
    }

    @Override
    public List<BlueprintEntryDto> getBlueprint(Long categoryId) {
        loadCategoryPort.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: id=" + categoryId));

        return loadBlueprintPort.findByCategoryId(categoryId).stream()
                .map(e -> new BlueprintEntryDto(
                        e.id(),
                        e.attributeKey(),
                        e.type(),
                        e.unitName(),
                        e.allowedValues(),
                        e.required(),
                        e.sortOrder()))
                .toList();
    }
}
