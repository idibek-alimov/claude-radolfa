package tj.radolfa.application.services.saga;

public interface SagaStep<C> {
    void execute(C context);
    void compensate(C context);

    default String name() {
        return getClass().getSimpleName();
    }
}
