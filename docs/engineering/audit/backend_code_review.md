# FAANG-Grade Backend Code Review: Project Radolfa

**Author**: Senior Fullstack Engineer (15+ YOE)  
**Objective**: Critical review of current backend implementation with a focus on scalability, security, and long-term maintainability.

---

## 1. High-Priority "Temporary" Fixes (Technical Debt)

### A. The "O(N) Memory" Scalability Wall
*   **Location**: `ProductController.java` (`getAllProducts`)
*   **Observation**: The controller calls `loadProductPort.loadAll()`, pulls the **entire** database into the Java Heap, and then performs `.stream().filter()` and `.skip().limit()` in memory.
*   **FAANG Perspective**: For 1,000 products, this is fast. For 100,000 products, this will trigger `OutOfMemoryError` and cause a production crash.
*   **Recommendation**: Move filtering and pagination to the **Database layer (JPA/SQL)** immediately. The Port should accept `Pageable` and `SearchSpecification` objects. Never pull more rows from the DB than you intend to show on the screen.

### B. Synchronous Media Bottleneck
*   **Location**: `AddProductImageService.java`
*   **Observation**: The `execute` method performs `imageProcessingPort.process` (Thumbnailator resize) and `imageUploadPort.upload` (S3 latency) within the main request thread.
*   **FAANG Perspective**: This blocks the Tomcat worker thread. If 20 managers upload images simultaneously, the whole API will become unresponsive for other users.
*   **Recommendation**: Implement a **Job-based approach**. Store the raw image, return a `202 Accepted`, and process the resizing/S3 upload in a background `@Async` worker or a separate microservice.

---

## 2. Security & Authentication Audit

### A. Lack of Brute-Force Protection
*   **Location**: `OtpAuthService.java`
*   **Observation**: There is no rate limiting on the `/api/v1/auth/login` (OTP generation) endpoint.
*   **Issue**: An attacker can spam this endpoint with a user's phone number, exhausting SMS credits (or filling logs) and annoying the user. 
*   **Recommendation**: Implement a **Leaky Bucket** or **Fixed Window** rate limiter (e.g., via Bucket4j or Redis). Limit a single IP to 3 OTP requests per hour.

### B. Security Context Leak
*   **Location**: `AuthController.java` (`/me` endpoint)
*   **Observation**: The endpoint extracts the `principal` and returns it directly.
*   **Issue**: While the current record is simple, if the `JwtAuthenticatedUser` is ever expanded to include internal metadata (e.g., session version, internal flags), these will be accidentally leaked to the frontend.
*   **Recommendation**: Always use a dedicated `UserDto`. Never return security credentials or internal records directly from the filter/context layer.

### C. Cookie Construction Concern
*   **Location**: `AuthController.java` (`buildAuthCookie`)
*   **Observation**: The controller is responsible for the string formatting and security flags of the JWT cookie.
*   **Issue**: This violates "Separation of Concerns." If you decide to change the cookie name or path, you have to search through controllers.
*   **Recommendation**: Move cookie construction to a `SecurityService` or `AuthService`. The controller should just deal with `ResponseEntity`.

---

## 3. Architectural Refinements (Hexagonal Integrity)

### A. Primitive Obsession in Domain
*   **Location**: `User.java` / `Product.java`
*   **Observation**: Fields like `phone` and `price` are strings and doubles.
*   **Recommendation**: Introduce **Value Objects**. `Phone` should validate itself during construction. `Price` should be a `Money` value object containing `BigDecimal` and `Currency` to avoid the floating-point precision issues inherent in `Double`.

### B. Transactional Boundaries
*   **Location**: `OtpAuthService.java`
*   **Observation**: `@Transactional` is on the `verify` method. 
*   **Issue**: If the `saveUserPort` succeeds but the `jwtUtil` fails (unlikely but possible), you might end up with a created user but a failed login. 
*   **Recommendation**: Ensure transactional boundaries are tightly defined around state changes.

---

## 4. Summary of Improvements (Roadmap)

1.  **Stop the Bleeding**: Migrate `ProductController` pagination to the database.
2.  **Harden the Gate**: Add Rate Limiting to the OTP flow.
3.  **Modernize the Pipeline**: Transition Image Processing to an asynchronous pattern.
4.  **Value Object Migration**: Refactor Domain models to use `Money` and `Phone` types before the database is populated with legacy primitive formats.

**Verdict**: The backend is well-structured for a V1, but its "In-Memory" habits will become its primary failure point within 6 months of growth. Refactor these patterns now while "losing data" is still an acceptable risk.
