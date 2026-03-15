package tj.radolfa.infrastructure.erp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

/**
 * Production HTTP adapter for {@link ErpProductClient}.
 *
 * Calls the ERPNext REST API to page through the Item doctype.
 * Authentication uses ERPNext token auth: {@code Authorization: token <key>:<secret>}.
 *
 * Gracefully returns an empty list on any connectivity or auth failure so
 * the application starts cleanly even before ERPNext credentials are configured.
 *
 * ERPNext API key and secret are generated in:
 *   ERPNext → Settings → User → Administrator → Generate API Key
 */
@Component
@Profile("!dev & !test")
public class ErpProductClientHttp implements ErpProductClient {

    private static final Logger log = LoggerFactory.getLogger(ErpProductClientHttp.class);

    private static final String ITEM_FIELDS =
            "[\"item_code\",\"item_name\",\"item_group\",\"standard_rate\",\"disabled\"]";

    private final RestClient restClient;
    private final boolean    credentialsConfigured;

    public ErpProductClientHttp(
            @Value("${erp.base-url:http://localhost:8000}") String baseUrl,
            @Value("${erp.api-key:}")                       String apiKey,
            @Value("${erp.api-secret:}")                    String apiSecret) {

        credentialsConfigured = !apiKey.isBlank() && !apiSecret.isBlank();

        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (credentialsConfigured) {
            builder.defaultHeader("Authorization", "token " + apiKey + ":" + apiSecret);
        }
        this.restClient = builder.build();

        if (!credentialsConfigured) {
            log.warn("[ERP] erp.api-key / erp.api-secret not configured — " +
                     "ERP sync will return empty until credentials are set.");
        }
    }

    @Override
    public List<ErpProductSnapshot> fetchPage(int page, int limit) {
        if (!credentialsConfigured) {
            return List.of();
        }

        int start = (page - 1) * limit;
        try {
            ErpItemListResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/resource/Item")
                            .queryParam("limit_page_length", limit)
                            .queryParam("limit_start", start)
                            .queryParam("fields", ITEM_FIELDS)
                            .queryParam("filters", "[[\"disabled\",\"=\",0]]")
                            .build())
                    .retrieve()
                    .body(ErpItemListResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                return List.of();
            }

            return response.data().stream()
                    .map(item -> new ErpProductSnapshot(
                            item.item_code(),
                            item.item_name(),
                            item.item_group() != null ? item.item_group() : "Uncategorized",
                            item.standard_rate() != null ? item.standard_rate() : BigDecimal.ZERO,
                            0,     // stock fetched separately via Bin when needed
                            item.disabled() != 0
                    ))
                    .toList();

        } catch (Exception e) {
            log.warn("[ERP] Failed to fetch page {} from ERPNext: {}", page, e.getMessage());
            return List.of();
        }
    }

    // ── Internal response DTOs ────────────────────────────────────────────────

    private record ErpItemListResponse(List<ErpItemRecord> data) {}

    private record ErpItemRecord(
            String     item_code,
            String     item_name,
            String     item_group,
            BigDecimal standard_rate,
            int        disabled
    ) {}
}
