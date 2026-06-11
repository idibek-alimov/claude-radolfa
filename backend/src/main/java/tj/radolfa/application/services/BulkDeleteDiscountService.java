package tj.radolfa.application.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.event.DiscountTargetsChangedEvent;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.in.discount.BulkDeleteDiscountUseCase;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.DiscountTarget;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BulkDeleteDiscountService implements BulkDeleteDiscountUseCase {

    private final LoadDiscountPort loadDiscountPort;
    private final SaveDiscountPort saveDiscountPort;
    private final ApplicationEventPublisher eventPublisher;

    public BulkDeleteDiscountService(LoadDiscountPort loadDiscountPort,
                                     SaveDiscountPort saveDiscountPort,
                                     ApplicationEventPublisher eventPublisher) {
        this.loadDiscountPort = loadDiscountPort;
        this.saveDiscountPort = saveDiscountPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public int execute(Command command) {
        List<DiscountTarget> affectedTargets = new ArrayList<>();
        for (Long id : command.ids()) {
            // Gather targets before deleting so the listener can still resolve
            // which products this campaign covered.
            loadDiscountPort.findById(id).ifPresent(d -> affectedTargets.addAll(d.targets()));
            saveDiscountPort.delete(id);
        }
        if (!affectedTargets.isEmpty()) {
            eventPublisher.publishEvent(new DiscountTargetsChangedEvent(affectedTargets));
        }
        return command.ids().size();
    }
}
