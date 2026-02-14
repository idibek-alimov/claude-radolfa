package tj.radolfa.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tj.radolfa.application.ports.out.LoadColorPort;
import tj.radolfa.application.ports.out.LoadColorPort.ColorView;
import tj.radolfa.application.ports.out.SaveColorPort;
import tj.radolfa.infrastructure.web.dto.ColorDto;
import tj.radolfa.infrastructure.web.dto.UpdateColorDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/colors")
public class ColorController {

    private final LoadColorPort loadColorPort;
    private final SaveColorPort saveColorPort;

    public ColorController(LoadColorPort loadColorPort, SaveColorPort saveColorPort) {
        this.loadColorPort = loadColorPort;
        this.saveColorPort = saveColorPort;
    }

    @GetMapping
    public ResponseEntity<List<ColorDto>> getAllColors() {
        List<ColorDto> colors = loadColorPort.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(colors);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ColorDto> updateColor(
            @PathVariable Long id,
            @RequestBody UpdateColorDto request) {

        ColorView existing = loadColorPort.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        ColorView updated = saveColorPort.update(id, request.displayName(), request.hexCode());
        return ResponseEntity.ok(toDto(updated));
    }

    private ColorDto toDto(ColorView view) {
        return new ColorDto(view.id(), view.colorKey(), view.displayName(), view.hexCode());
    }
}
