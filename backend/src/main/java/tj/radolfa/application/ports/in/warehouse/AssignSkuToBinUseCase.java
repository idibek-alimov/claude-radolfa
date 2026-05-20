package tj.radolfa.application.ports.in.warehouse;

public interface AssignSkuToBinUseCase {

    /** Assigns the SKU to the given bin. Pass {@code null} for binId to unassign. */
    void execute(Long skuId, Long binId);
}
