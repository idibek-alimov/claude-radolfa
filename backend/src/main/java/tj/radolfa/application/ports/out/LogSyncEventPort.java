package tj.radolfa.application.ports.out;

/**
 * Out-Port: record a single ERP sync event (success or error) into the
 * {@code erp_sync_log} audit table.
 */
public interface LogSyncEventPort {

    /**
     * @param erpId        the ERPNext product identifier
     * @param success      {@code true} if the sync completed without error
     * @param errorMessage non-null only when {@code success} is {@code false}
     */
    void log(String erpId, boolean success, String errorMessage);
}
