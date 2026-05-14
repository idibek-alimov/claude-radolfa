package tj.radolfa.application.ports.in.discount;

import tj.radolfa.domain.model.TopCampaignRow;

import java.util.List;

public interface GetTopCampaignsUseCase {

    List<TopCampaignRow> execute(Command command);

    record Command(String by, String period, int limit) {}
}
