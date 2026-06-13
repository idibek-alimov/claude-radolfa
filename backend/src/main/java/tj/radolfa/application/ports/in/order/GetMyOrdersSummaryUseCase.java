package tj.radolfa.application.ports.in.order;

public interface GetMyOrdersSummaryUseCase {
    MyOrdersSummary execute(Long userId);
}
