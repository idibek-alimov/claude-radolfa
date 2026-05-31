package tj.radolfa.domain.exception;

/**
 * Thrown whenever code attempts to overwrite a field that is managed by an
 * authoritative source and cannot be modified directly, or when the caller
 * lacks ownership of the resource being edited.
 */
public class FieldLockException extends RuntimeException {

    public FieldLockException(String field) {
        super("Field '" + field + "' is managed by an authoritative source and cannot be modified directly.");
    }

    public FieldLockException(String field, String reason) {
        super("Field '" + field + "' cannot be modified: " + reason);
    }
}
