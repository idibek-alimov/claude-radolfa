package tj.radolfa.application.ports.in.review;

public interface DeleteReviewTraitUseCase {

    /**
     * Deletes a review trait by ID.
     * All category links are removed automatically via the database cascade.
     *
     * @param id trait ID
     */
    void execute(Long id);
}
