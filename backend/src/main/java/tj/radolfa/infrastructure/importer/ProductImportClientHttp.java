package tj.radolfa.infrastructure.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

/**
 * Production HTTP adapter for {@link ProductImportClient}.
 *
 * Calls the external catalogue REST API to page through the Item doctype.
 * Authentication uses token auth: {@code Authorization: token <key>:<secret>}.
 *
 * Gracefully returns an empty list on any connectivity or auth failure so
 * the application starts cleanly even before credentials are configured.
 */
@Component
@Profile("!dev & !test")
public class ProductImportClientHttp implements ProductImportClient {

    private static final Logger log = LoggerFactory.getLogger(ProductImportClientHttp.class);

    private static final String ITEM_FIELDS =
            "[\"item_code\",\"item_name\",\"item_group\",\"standard_rate\",\"disabled\"]";

    private final RestClient restClient;
    private final boolean    credentialsConfigured;

    public ProductImportClientHttp(
            @Value("${importer.base-url:http://localhost:8000}") String baseUrl,
            @Value("${importer.api-key:}")                       String apiKey,
            @Value("${importer.api-secret:}")                    String apiSecret) {

        credentialsConfigured = !apiKey.isBlank() && !apiSecret.isBlank();

        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (credentialsConfigured) {
            builder.defaultHeader("Authorization", "token " + apiKey + ":" + apiSecret);
        }
        this.restClient = builder.build();

        if (!credentialsConfigured) {
            log.warn("[IMPORTER] api-key / api-secret not configured — " +
                     "product import will return empty until credentials are set.");
        }
    }

    @Override
    public List<ImportedProductSnapshot> fetchPage(int page, int limit) {
        if (!credentialsConfigured) {
            return List.of();
        }

        int start = (page - 1) * limit;
        try {
            ItemListResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/resource/Item")
                            .queryParam("limit_page_length", limit)
                            .queryParam("limit_start", start)
                            .queryParam("fields", ITEM_FIELDS)
                            .queryParam("filters", "[[\"disabled\",\"=\",0]]")
                            .build())
                    .retrieve()
                    .body(ItemListResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                return List.of();
            }

            return response.data().stream()
                    .map(item -> new ImportedProductSnapshot(
                            item.item_code(),
                            item.item_name(),
                            item.item_group() != null ? item.item_group() : "Uncategorized",
                            item.standard_rate() != null ? item.standard_rate() : BigDecimal.ZERO,
                            0,     // stock fetched separately via Bin when needed
                            item.disabled() != 0
                    ))
                    .toList();

        } catch (Exception e) {
            log.warn("[IMPORTER] Failed to fetch page {} from catalogue: {}", page, e.getMessage());
            return List.of();
        }
    }

    // ── Internal response DTOs ────────────────────────────────────────────────

    private record ItemListResponse(List<ItemRecord> data) {}

    private record ItemRecord(
            String     item_code,
            String     item_name,
            String     item_group,
            BigDecimal standard_rate,
            int        disabled
    ) {}
}
