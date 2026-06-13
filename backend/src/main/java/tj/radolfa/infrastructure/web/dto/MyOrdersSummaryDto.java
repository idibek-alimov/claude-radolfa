package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.ports.in.order.MyOrdersSummary;

public record MyOrdersSummaryDto(long all, long progress, long delivered, long returns) {

    public static MyOrdersSummaryDto from(MyOrdersSummary summary) {
        return new MyOrdersSummaryDto(summary.all(), summary.progress(), summary.delivered(), summary.returns());
    }
}
