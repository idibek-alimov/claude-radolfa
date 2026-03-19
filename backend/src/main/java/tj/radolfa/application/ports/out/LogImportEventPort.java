package tj.radolfa.application.ports.out;

/**
 * Out-Port: record a single import event (success or error) into the
 * {@code sync_log} audit table.
 */
public interface LogImportEventPort {

    /**
     * @param importId     the external product/entity identifier
     * @param success      {@code true} if the import completed without error
     * @param errorMessage non-null only when {@code success} is {@code false}
     */
    void log(String importId, boolean success, String errorMessage);
}
