package tj.radolfa.infrastructure.erp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Production HTTP adapter for {@link ErpProductClient}.
 *
 * <p>Fetches Item templates and their variants from ERPNext REST API.
 * Authentication uses ERPNext token auth: {@code Authorization: token <key>:<secret>}.
 *
 * <p>Strategy: fetches ALL enabled items in one pass. The processor groups
 * them by {@code variant_of} to build the template→variant hierarchy.
 */
@Component
@Profile("!dev & !test")
public class ErpProductClientHttp implements ErpProductClient {

    private static final Logger log = LoggerFactory.getLogger(ErpProductClientHttp.class);

    private static final String ITEM_FIELDS =
            "[\"item_code\",\"item_name\",\"item_group\",\"standard_rate\"," +
            "\"disabled\",\"has_variants\",\"variant_of\"]";

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
                            0,
                            item.disabled() != 0,
                            item.has_variants() != 0,
                            item.variant_of(),
                            Map.of()  // attributes resolved in processor via separate call
                    ))
                    .toList();

        } catch (Exception e) {
            log.warn("[ERP] Failed to fetch page {} from ERPNext: {}", page, e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetches variant attributes for a specific item code.
     * Returns a map like {"Color": "Red", "Size": "M"}.
     */
    public Map<String, String> fetchVariantAttributes(String itemCode) {
        if (!credentialsConfigured) {
            return Map.of();
        }
        try {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/resource/Item/{code}")
                            .queryParam("fields", "[\"attributes\"]")
                            .build(itemCode))
                    .retrieve()
                    .body(ErpItemDetailResponse.class);

            if (response == null || response.data() == null || response.data().attributes() == null) {
                return Map.of();
            }

            return response.data().attributes().stream()
                    .collect(Collectors.toMap(
                            ErpItemAttribute::attribute,
                            ErpItemAttribute::attribute_value,
                            (a, b) -> b  // keep last on duplicate key
                    ));

        } catch (Exception e) {
            log.warn("[ERP] Failed to fetch attributes for {}: {}", itemCode, e.getMessage());
            return Map.of();
        }
    }

    // ── Internal response DTOs ────────────────────────────────────────────────

    private record ErpItemListResponse(List<ErpItemRecord> data) {}

    private record ErpItemRecord(
            String     item_code,
            String     item_name,
            String     item_group,
            BigDecimal standard_rate,
            int        disabled,
            int        has_variants,
            String     variant_of
    ) {}

    private record ErpItemDetailResponse(ErpItemDetail data) {}

    private record ErpItemDetail(List<ErpItemAttribute> attributes) {}

    private record ErpItemAttribute(String attribute, String attribute_value) {}
}
