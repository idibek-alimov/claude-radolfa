# Tech Debt & Future Improvements

Items discovered during audit fixes that should be addressed later.

---

### 1. Cache user-enabled check in JwtAuthenticationFilter
- **Context:** Fix for issue 5.1 (blocked users bypass) added a `LoadUserPort.loadById()` call on every authenticated request. This hits the database per request.
- **Improvement:** Add a short-lived cache (e.g., Caffeine, 1-2 min TTL) to avoid a DB query on every request. Invalidate on user block/unblock.
- **Priority:** LOW (current load is fine for single-VPS deployment)

### 2. Add "pending sync" table for skipped orders
- **Context:** Fix for issue 6.3 (silent order skip) now returns a `SyncResult` with SKIPPED status and a 422 response so the ERP caller knows to retry. However, there's no server-side persistence of skipped orders for reconciliation.
- **Improvement:** Create an `erp_pending_sync` table that stores skipped order payloads. Add a scheduled job or admin endpoint to retry pending syncs once the missing user is created.
- **Priority:** MEDIUM (depends on how often users are created after their first order)
