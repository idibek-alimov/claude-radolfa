package tj.radolfa.domain.exception;

/**
 * Thrown whenever code attempts to overwrite a field that is locked
 * by ERPNext (name, price, stock) outside of the authorised ERP sync path.
 */
public class ErpLockViolationException extends RuntimeException {

    public ErpLockViolationException(String field) {
        super("Field '" + field + "' is locked by ERP and cannot be modified.");
    }
}
