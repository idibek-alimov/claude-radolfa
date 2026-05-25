package tj.radolfa.application.ports.out;

public interface AssignSkuToBinPort {

    /** Assigns the SKU to the given bin. Pass {@code null} for binId to unassign. */
    void assign(Long skuId, Long binId);
}
