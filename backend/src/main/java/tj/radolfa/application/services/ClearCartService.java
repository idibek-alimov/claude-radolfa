package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.cart.ClearCartUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.SaveCartPort;

@Service
public class ClearCartService implements ClearCartUseCase {

    private final LoadCartPort loadCartPort;
    private final SaveCartPort saveCartPort;

    public ClearCartService(LoadCartPort loadCartPort, SaveCartPort saveCartPort) {
        this.loadCartPort = loadCartPort;
        this.saveCartPort = saveCartPort;
    }

    @Override
    @Transactional
    public void execute(Long userId) {
        loadCartPort.findActiveByUserId(userId).ifPresent(cart -> {
            cart.clear();
            saveCartPort.save(cart);
        });
    }
}
