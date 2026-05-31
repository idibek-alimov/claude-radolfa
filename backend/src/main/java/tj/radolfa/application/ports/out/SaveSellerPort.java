package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Seller;

public interface SaveSellerPort {
    Seller save(Seller seller);
}
