package tj.radolfa.application.ports.in.seller;

import tj.radolfa.domain.model.Seller;

public interface GetMySellerProfileUseCase {

    /**
     * Returns the seller profile for the given user.
     *
     * @throws tj.radolfa.domain.exception.ResourceNotFoundException if the user has no seller profile
     */
    Seller execute(Long userId);
}
