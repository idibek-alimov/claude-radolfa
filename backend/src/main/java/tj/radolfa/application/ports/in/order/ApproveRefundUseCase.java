package tj.radolfa.application.ports.in.order;

public interface ApproveRefundUseCase {
    void execute(Long returnId, Long adminUserId);
}
