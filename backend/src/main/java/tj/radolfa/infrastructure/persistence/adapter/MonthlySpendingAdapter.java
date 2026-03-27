package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadMonthlySpendingPort;
import tj.radolfa.infrastructure.persistence.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class MonthlySpendingAdapter implements LoadMonthlySpendingPort {

    private final PaymentRepository paymentRepository;

    public MonthlySpendingAdapter(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public BigDecimal calculateNetSpending(Long userId, Instant from, Instant to) {
        BigDecimal result = paymentRepository.calculateNetSpending(userId, from, to);
        return result != null ? result : BigDecimal.ZERO;
    }
}
