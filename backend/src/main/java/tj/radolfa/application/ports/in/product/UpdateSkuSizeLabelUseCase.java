package tj.radolfa.application.ports.in.product;

/**
 * In-Port: update the size label of a specific SKU.
 *
 * <p>Called by MANAGER or ADMIN. The size label is a display-only field
 * and is not authoritative-source-locked, so it can be updated freely.
 */
public interface UpdateSkuSizeLabelUseCase {

    /** @param skuId        the ID of the SKU to update
     *  @param newSizeLabel the new human-readable size label (e.g. "XL", "42") */
    void execute(Long skuId, String newSizeLabel);
}
