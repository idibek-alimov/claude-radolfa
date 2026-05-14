package tj.radolfa.application.ports.in.discount.type;

public interface DeleteDiscountTypeUseCase {

    /** Throws {@link tj.radolfa.domain.exception.DiscountTypeInUseException} if any discounts reference this type. */
    void execute(Long id);
}
