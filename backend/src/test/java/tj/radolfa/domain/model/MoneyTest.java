package tj.radolfa.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    @DisplayName("Constructor rejects null amount")
    void constructor_rejectsNull() {
        assertThrows(NullPointerException.class, () -> new Money(null));
    }

    @Test
    @DisplayName("Constructor rejects negative amount")
    void constructor_rejectsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new Money(new BigDecimal("-0.01")));
    }

    @Test
    @DisplayName("Constructor accepts zero")
    void constructor_acceptsZero() {
        Money m = new Money(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, m.amount());
    }

    @Test
    @DisplayName("Constructor accepts positive amount")
    void constructor_acceptsPositive() {
        Money m = new Money(new BigDecimal("99.99"));
        assertEquals(new BigDecimal("99.99"), m.amount());
    }

    @Test
    @DisplayName("Money.of returns ZERO for null input")
    void of_returnsZeroForNull() {
        Money m = Money.of(null);
        assertEquals(BigDecimal.ZERO, m.amount());
    }

    @Test
    @DisplayName("Money.of wraps non-null input")
    void of_wrapsNonNull() {
        Money m = Money.of(new BigDecimal("49.50"));
        assertEquals(new BigDecimal("49.50"), m.amount());
    }

    @Test
    @DisplayName("add returns correct sum")
    void add_returnsSum() {
        Money a = new Money(new BigDecimal("10.00"));
        Money b = new Money(new BigDecimal("5.50"));

        assertEquals(new BigDecimal("15.50"), a.add(b).amount());
    }

    @Test
    @DisplayName("multiply returns correct product")
    void multiply_returnsProduct() {
        Money m = new Money(new BigDecimal("20.00"));
        assertEquals(new BigDecimal("60.00"), m.multiply(3).amount());
    }

    @Test
    @DisplayName("ZERO constant has amount = 0")
    void zeroConstant_isZero() {
        assertEquals(BigDecimal.ZERO, Money.ZERO.amount());
    }

    @Test
    @DisplayName("Two Money instances with the same amount are equal (record equality)")
    void equality_byAmount() {
        Money a = new Money(new BigDecimal("25.00"));
        Money b = new Money(new BigDecimal("25.00"));
        assertEquals(a, b);
    }
}
