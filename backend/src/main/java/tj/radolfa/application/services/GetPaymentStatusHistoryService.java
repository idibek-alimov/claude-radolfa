package tj.radolfa.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.payment.GetPaymentStatusHistoryUseCase;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.LoadPaymentStatusChangePort;
import tj.radolfa.domain.model.PaymentStatusChange;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetPaymentStatusHistoryService implements GetPaymentStatusHistoryUseCase {

    private final LoadPaymentPort              loadPaymentPort;
    private final LoadPaymentStatusChangePort  loadPaymentStatusChangePort;

    public GetPaymentStatusHistoryService(LoadPaymentPort loadPaymentPort,
                                          LoadPaymentStatusChangePort loadPaymentStatusChangePort) {
        this.loadPaymentPort             = loadPaymentPort;
        this.loadPaymentStatusChangePort = loadPaymentStatusChangePort;
    }

    @Override
    public Page<PaymentStatusChange> execute(Long orderId, Pageable pageable) {
        return loadPaymentPort.findByOrderId(orderId)
                .map(payment -> loadPaymentStatusChangePort.findByPaymentId(payment.id(), pageable))
                .orElse(new PageImpl<>(List.of(), pageable, 0));
    }
}
