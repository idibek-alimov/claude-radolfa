# Backend Refinement 01: Database-Powered Pagination (Critical)

### PERSONA
You are a **Senior Software Engineer at a FAANG company**. You are an expert in high-performance Java backends and database optimization. You identify O(N) memory leaks and architectural bottlenecks before they cause production outages.

### OBJECTIVE
Migrate the product listing logic from "In-Memory Filtering/Pagination" to "Database-Layer Pagination" to prevent 500 errors and OutOfMemory crashes as the catalog grows.

### INSTRUCTIONS
1.  **Analyze & Plan**: Examine the target files. Specifically, note how `loadProductPort.loadAll()` pulls the entire database into memory.
2.  **Suggest Fix**: Propose a plan that:
    - Updates the `LoadProductPort` to accept pagination parameters (page, limit).
    - Implement a **Basic SQL Filter** for the `search` parameter as an Admin-level fallback (exact matches only). **Note**: Discovery-grade fuzzy search will be handled in Phase 06 via Elasticsearch.
    - Refactors the `JpaProductAdapter` to use Spring Data JPA's `Pageable` and `Specification` (or Query methods) to perform filtering and pagination at the SQL level.
    - Updates the `ProductController` to pass these parameters down through the application layer.
3.  **Wait for Approval**: Do not write code until the user approves your proposed JPA repository design.
4.  **Execute**: Implement the approved changes and verify that the API still returns the same `PaginatedProducts` structure but with significantly lower memory overhead.

### WHERE TO LOOK
- **Controller**: `backend/src/main/java/tj/radolfa/infrastructure/web/ProductController.java`
- **Port**: `backend/src/main/java/tj/radolfa/application/ports/out/LoadProductPort.java`
- **Adapter**: `backend/src/main/java/tj/radolfa/infrastructure/persistence/JpaProductAdapter.java` (or similar)
- **Entity/Repo**: `backend/src/main/java/tj/radolfa/infrastructure/persistence/repository/ProductRepository.java`

### CONSTRAINTS
- **Infrastructure Safety**: Use `spring-boot-starter-data-jpa` features rather than writing raw, unescaped SQL.
- **Hexagonal Integrity**: Keep the pagination domain logic (like the `Page` object or custom DTOs) decoupled from JPA-specific classes in the application services.
---
