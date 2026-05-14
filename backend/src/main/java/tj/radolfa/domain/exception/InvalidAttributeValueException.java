package tj.radolfa.domain.exception;

/**
 * Thrown when an attribute value violates the blueprint type constraints
 * (e.g. an ENUM value not in the allowed list, or a non-numeric NUMBER value).
 *
 * <p>Pure Java — zero Spring / JPA / Jackson dependencies.
 */
public class InvalidAttributeValueException extends RuntimeException {

    public InvalidAttributeValueException(String attributeKey, String reason) {
        super("Invalid value for attribute '" + attributeKey + "': " + reason);
    }
}
