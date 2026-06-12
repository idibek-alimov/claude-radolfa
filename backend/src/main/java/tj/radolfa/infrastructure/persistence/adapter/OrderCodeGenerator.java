package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Generates human-friendly order codes for new native orders.
 *
 * <p>Format: {@code ORD-XXXXX} where XXXXX is a zero-padded 5-digit number
 * drawn from the {@code order_external_code_seq} PostgreSQL sequence.
 * Examples: {@code ORD-10001}, {@code ORD-10047}, {@code ORD-99999}.
 *
 * <p>Pure infrastructure concern — never referenced by the domain or application layers.
 * Imported orders that already carry an {@code externalOrderId} are left untouched.
 */
@Component
class OrderCodeGenerator {

    private final JdbcTemplate jdbc;

    OrderCodeGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    String generate() {
        Long seq = jdbc.queryForObject(
                "SELECT NEXTVAL('order_external_code_seq')", Long.class);
        return "ORD-" + String.format("%05d", seq);
    }
}
