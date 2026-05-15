package tj.radolfa.domain.service;

import java.security.SecureRandom;

public class BarcodeGenerator {

    private static final String PREFIX = "200";
    private final SecureRandom random;

    public BarcodeGenerator() {
        this(new SecureRandom());
    }

    BarcodeGenerator(SecureRandom random) {
        this.random = random;
    }

    /** Returns a valid 13-digit EAN-13 barcode using the GS1 restricted-circulation prefix 200. */
    public String next() {
        StringBuilder sb = new StringBuilder(13).append(PREFIX);
        for (int i = 0; i < 9; i++) sb.append(random.nextInt(10));
        sb.append(checkDigit(sb));
        return sb.toString();
    }

    public static int checkDigit(CharSequence twelve) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = twelve.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        return (10 - sum % 10) % 10;
    }
}
