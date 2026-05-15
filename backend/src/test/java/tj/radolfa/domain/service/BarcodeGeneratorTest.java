package tj.radolfa.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BarcodeGeneratorTest {

    private final BarcodeGenerator generator = new BarcodeGenerator();

    @Test
    @DisplayName("next(): output is exactly 13 characters")
    void next_isThirteenChars() {
        assertEquals(13, generator.next().length());
    }

    @Test
    @DisplayName("next(): output is all digits")
    void next_isAllDigits() {
        String barcode = generator.next();
        assertTrue(barcode.matches("\\d{13}"), "Expected all digits, got: " + barcode);
    }

    @Test
    @DisplayName("next(): output starts with restricted-circulation prefix '200'")
    void next_startsWithPrefix200() {
        assertTrue(generator.next().startsWith("200"));
    }

    @RepeatedTest(100)
    @DisplayName("next(): check digit passes EAN-13 mod-10 validation every run")
    void next_checkDigitIsValid() {
        String barcode = generator.next();
        int expected = BarcodeGenerator.checkDigit(barcode.substring(0, 12));
        assertEquals(expected, barcode.charAt(12) - '0',
                "Invalid check digit in barcode: " + barcode);
    }

    @Test
    @DisplayName("next(): 10 000 consecutive calls produce no duplicates")
    void next_noCollisionsOver10000Calls() {
        Set<String> seen = new HashSet<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            assertTrue(seen.add(generator.next()), "Duplicate barcode detected at call " + i);
        }
    }

    @Test
    @DisplayName("checkDigit(): known EAN-13 sample '978014300723' → 4")
    void checkDigit_knownSample() {
        // Manually verified: 2×1+0×3+0×1+… = 86, (10−86%10)%10 = 4
        assertEquals(4, BarcodeGenerator.checkDigit("978014300723"));
    }

    @Test
    @DisplayName("checkDigit(): all-zeros input → check digit 0")
    void checkDigit_allZeros() {
        assertEquals(0, BarcodeGenerator.checkDigit("000000000000"));
    }
}
