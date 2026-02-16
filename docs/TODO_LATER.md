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

### 3. Token blacklist for immediate revocation
- **Context:** The refresh token implementation (fix #14) rotates tokens on each refresh and checks user enabled status from DB. However, if a user is blocked mid-session, their current access token remains valid until it expires (up to 15 minutes).
- **Improvement:** Add a Redis-based token blacklist. When a user is blocked or their role changes, add their current token `jti` to the blacklist. The `JwtAuthenticationFilter` would check the blacklist on each request.
- **Priority:** LOW (15-minute window is acceptable for most cases; the enabled check on refresh already prevents new tokens)

### 4. Migrate OTP and rate limiter storage to Redis
- **Context:** Both `OtpStore` and `RateLimiterService` use in-memory `ConcurrentHashMap`. Works for single-VPS but breaks in multi-instance deployment (OTP verified on wrong instance, rate limits not shared).
- **Improvement:** Move both to Redis-backed implementations when scaling beyond a single instance.
- **Priority:** LOW (single-VPS deployment for now)
