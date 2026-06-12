package tj.radolfa.application.ports.in.seller;

import tj.radolfa.domain.model.Seller;

public interface CreateSellerUseCase {

    record Command(String phone, String shopName, String logoUrl, String bio) {}

    Seller execute(Command command);
}
