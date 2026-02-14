# Secondary Backend Audit Report

Following the initial audit, a deeper dive into the infrastructure and security layers has identified the following areas for improvement or potential bugs.

## 1. Memory Leak in `OtpStore`
**Severity: Low/Medium**

- **Issue**: The `OtpStore.java` class maintains an in-memory `ConcurrentHashMap` of OTPs. While it has a `cleanupExpired()` method to evict old entries, **this method is never called**.
- **Impact**: Every request for an OTP that goes unverified will consume memory indefinitely. Over long periods or under automated attacks, this could lead to `OutOfMemoryError`.
- **Recommendation**: Add a `@Scheduled` annotation to `cleanupExpired()` to run periodically (e.g., every 5-10 minutes).

## 2. Incomplete Search Index Data
**Severity: Medium**

- **Issue**: `ListingSearchAdapter.java` (lines 138-146) hard-codes `category`, `colorHexCode`, and `topSelling` fields to `null` or `false` when mapping to DTOs.
- **Impact**: The frontend "Catalog" and "Search" results will lack these critical data points, breaking the UI's ability to show color swatches or category labels for items found via search.
- **Recommendation**: Update the `ListingDocument` to include these fields and ensure they are populated during the `index()` operation.

## 3. Scalability Constraint (Horizontal Scaling)
**Severity: Medium (Architectural)**

- **Issue**: Both `OtpStore` and `RateLimiterService` use in-memory maps.
- **Impact**: If the application is ever deployed with multiple instances (e.g., in Kubernetes or behind a load balancer), the OTP requested on Instance A will not be verifiable on Instance B. Similarly, rate limits will be applied per-instance rather than globally.
- **Recommendation**: Plan to migrate these stores to **Redis** for production-grade horizontal scalability.

## 4. Memory Pressure on Image Uploads
**Severity: Low/Medium**

- **Issue**: `S3ImageUploader` (line 43) and `ThumbnailatorImageProcessor` (line 64) read entire image streams into memory as byte arrays.
- **Impact**: Simultaneously uploading several large original images could spike the JVM heap memory and potentially cause crashes.
- **Recommendation**: Use streaming where possible (e.g., `RequestBody.fromInputStream`) or enforce strict client-side and server-side file size limits.

## 5. Hard-coded Variant Logic
**Severity: Low**

- **Issue**: `ErpProductProcessor.java` (line 46) hard-codes the variant code to `"default"`.
- **Impact**: This limits the system's ability to handle items that actually have multiple variants (e.g., different sizes) coming from the ERP under the same template code.
- **Recommendation**: Refactor to extract actual variant identifiers if the ERP schema supports them.

---
**Next Steps**: I can provide an implementation plan to fix items 1 and 2 immediately, as they impact current stability and frontend functionality.
