package tj.radolfa.application.ports.in.product;

/**
 * In-Port: rename a product (updates ProductBase.name).
 *
 * <p>Called by MANAGER or ADMIN. When the external importer is active,
 * this name will be overwritten on the next import cycle; use this only
 * when the app is the authoritative source.
 */
public interface UpdateProductNameUseCase {

    /** @param productBaseId the ID of the ProductBase to rename */
    void execute(Long productBaseId, String newName);
}
