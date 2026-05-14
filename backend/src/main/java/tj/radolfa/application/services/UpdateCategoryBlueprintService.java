package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdateCategoryBlueprintUseCase;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort;
import tj.radolfa.application.ports.out.LoadCategoryBlueprintPort.BlueprintEntry;
import tj.radolfa.application.ports.out.SaveCategoryBlueprintPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.AttributeType;

@Service
public class UpdateCategoryBlueprintService implements UpdateCategoryBlueprintUseCase {

    private final LoadCategoryBlueprintPort loadBlueprintPort;
    private final SaveCategoryBlueprintPort saveBlueprintPort;

    public UpdateCategoryBlueprintService(LoadCategoryBlueprintPort loadBlueprintPort,
                                          SaveCategoryBlueprintPort saveBlueprintPort) {
        this.loadBlueprintPort = loadBlueprintPort;
        this.saveBlueprintPort = saveBlueprintPort;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        // Verify blueprint exists under the given category
        BlueprintEntry existing = loadBlueprintPort.findByCategoryId(command.categoryId())
                .stream()
                .filter(e -> e.id().equals(command.blueprintId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Blueprint not found: id=" + command.blueprintId() + " for categoryId=" + command.categoryId()));

        // attributeKey uniqueness within the category, excluding self
        boolean keyConflict = loadBlueprintPort.findByCategoryId(command.categoryId())
                .stream()
                .anyMatch(e -> e.attributeKey().equals(command.attributeKey()) && !e.id().equals(command.blueprintId()));
        if (keyConflict) {
            throw new IllegalArgumentException(
                    "Attribute key '" + command.attributeKey() + "' already exists in this category");
        }

        AttributeType existingType = existing.type();

        // allowedValues only valid for ENUM/MULTI; unitName only valid for NUMBER
        boolean hasAllowedValues = command.allowedValues() != null && !command.allowedValues().isEmpty();
        if (hasAllowedValues && existingType != AttributeType.ENUM && existingType != AttributeType.MULTI) {
            throw new IllegalArgumentException("Allowed values are only applicable to ENUM or MULTI attributes");
        }
        boolean hasUnitName = command.unitName() != null && !command.unitName().isBlank();
        if (hasUnitName && existingType != AttributeType.NUMBER) {
            throw new IllegalArgumentException("Unit name is only applicable to NUMBER attributes");
        }

        saveBlueprintPort.update(command);
    }
}
