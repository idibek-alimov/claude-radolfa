package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Generates article codes for new listing variants.
 *
 * <p>Format: zero-padded 5-digit number drawn from the
 * {@code listing_variant_code_seq} PostgreSQL sequence.
 * Examples: {@code 10001}, {@code 10047}, {@code 99999}.
 *
 * <p>Pure infrastructure concern — never referenced by the domain or application layers.
 */
@Component
class ProductCodeGenerator {

    private final JdbcTemplate jdbc;

    ProductCodeGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    String generate() {
        Long seq = jdbc.queryForObject(
                "SELECT NEXTVAL('listing_variant_code_seq')", Long.class);
        return String.format("%05d", seq);
    }
}
