# Tech Debt & Future Improvements

Items discovered during audit fixes that should be addressed later.

---

### 1. Cache user-enabled check in JwtAuthenticationFilter
- **Context:** Fix for issue 5.1 (blocked users bypass) added a `LoadUserPort.loadById()` call on every authenticated request. This hits the database per request.
- **Improvement:** Add a short-lived cache (e.g., Caffeine, 1-2 min TTL) to avoid a DB query on every request. Invalidate on user block/unblock.
- **Priority:** LOW (current load is fine for single-VPS deployment)
