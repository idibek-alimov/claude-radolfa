# Backend Refinement 06: Elasticsearch Search Engine (High Scale)

### PERSONA
You are a **Senior Software Engineer at a FAANG company**. You specialize in search infrastructure and distributed indexing. You understand that for large catalogs, SQL `LIKE` queries are unacceptable for both performance and user experience reasons.

### OBJECTIVE
Implement a high-performance, fuzzy search engine for the product catalog using the existing (but currently idle) Elasticsearch infrastructure.

### INSTRUCTIONS
1.  **Analyze & Plan**: 
    - Review the "In-Memory Filtering" logic in `ProductController.java`.
    - Check the Elasticsearch connection settings in `application.yml`.
2.  **Suggest Fix**: Propose a plan that:
    - Creates a `ProductDocument` (Elasticsearch document entity).
    - Implements a **Search Sync pattern**: Either a `ProductListener` (Hibernate/JPA) or a direct update in the `JpaProductAdapter` to sync the index whenever a product is created or updated in PostgreSQL.
    - Implements a `SearchProductsUseCase` that queries Elasticsearch instead of the database.
    - Adds advanced features: **Fuzzy Matching** (to handle typos) and **Autocomplete** support.
3.  **Wait for Approval**: Do not start indexing data until the user approves the mapping (which fields to index) and the sync strategy (listener vs. explicit).
4.  **Execute**: Implement the approved search engine and wire the `ProductController` (or a dedicated `SearchController`) to use it. 
    - **Architecture Note**: This phase replaces the "Basic SQL Filter" from Phase 01 for user-facing discovery requests, while the Postgres pagination from Phase 01 remains the source of truth for the internal Management Table.

### WHERE TO LOOK
- **Configs**: `backend/src/main/resources/application.yml`
- **Controller**: `backend/src/main/java/tj/radolfa/infrastructure/web/ProductController.java`
- **Persistence Adapter**: `backend/src/main/java/tj/radolfa/infrastructure/persistence/JpaProductAdapter.java`

### CONSTRAINTS
- **Resiliency**: The application must not crash if Elasticsearch is offline; it should fallback to a basic JPA search or return a graceful 503 error for search-only requests.
- **Synchronization**: Ensure that deleted products are also removed from the Elasticsearch index (Soft Delete support recommended).
---
