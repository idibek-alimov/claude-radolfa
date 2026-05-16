package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.user.ListCouriersUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.List;

@Service
public class ListCouriersService implements ListCouriersUseCase {

    private final LoadUserPort loadUserPort;

    public ListCouriersService(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public List<User> execute() {
        return loadUserPort.findByRoleAndEnabledTrue(UserRole.COURIER);
    }
}
