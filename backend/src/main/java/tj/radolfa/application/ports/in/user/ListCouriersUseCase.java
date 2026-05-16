package tj.radolfa.application.ports.in.user;

import tj.radolfa.domain.model.User;

import java.util.List;

public interface ListCouriersUseCase {
    List<User> execute();
}
