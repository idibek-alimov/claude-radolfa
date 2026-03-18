package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Generates human-friendly product codes for new listing variants.
 *
 * <p>Format: {@code RD-XXXXX} where XXXXX is a zero-padded 5-digit number
 * drawn from the {@code listing_variant_code_seq} PostgreSQL sequence.
 * Examples: {@code RD-10001}, {@code RD-10047}, {@code RD-99999}.
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
        return "RD-" + String.format("%05d", seq);
    }
}
