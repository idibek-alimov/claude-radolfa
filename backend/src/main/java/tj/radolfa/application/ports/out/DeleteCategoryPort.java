package tj.radolfa.application.ports.out;

/**
 * Out-Port: delete a product category.
 */
public interface DeleteCategoryPort {

    /**
     * Deletes the category with the given ID.
     *
     * @throws IllegalStateException if the category is still assigned to products
     */
    void deleteById(Long categoryId);
}
