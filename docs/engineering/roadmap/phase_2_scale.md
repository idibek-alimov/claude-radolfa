# Roadmap Phase 2: Scale (Search & Reliability)

### PERSONA
You are a **Senior Fullstack Engineer (FAANG)** specializing in distributed systems and database integrity. You are tasked with making the catalog horizontally scalable and concurrent-safe.

### TASK
1.  **Implement Optimistic Locking** for the Product domain.
2.  **Integrate Elasticsearch** for high-performance product discovery.

### SCOPE & TARGETS
- **Persistence**: `backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/ProductEntity.java`.
- **Search Infrastructure**: `tj.radolfa.infrastructure.elasticsearch.ProductSearchRepository`.
- **Workflow**: `tj.radolfa.application.services.UpdateProductService`.

### REQUIREMENTS

#### 1. Reliability: Optimistic Locking
- **Backend**:
  - Add an `@Version` field to `ProductEntity`.
  - Update any DB migrations required (Flyway) to include a `version` column (default 0).
  - Ensure that the frontend receives an `HTTP 409 Conflict` if an update fails due to a version mismatch.
  - Update `UpdateProductRequestDto` to include the `version` field for safe round-tripping.

#### 2. Scalability: Elasticsearch Sync
- **Infrastructure**: Configure the `ElasticsearchClient` using existing properties in `application.yml`.
- **Indexing**: 
  - Create a `ProductDocument` mirroring the fields needed for search (Name, Description, ERP ID).
  - Implement a `ProductSyncListener` or update the persistence adapter to save the product to Elasticsearch whenever it is saved to PostgreSQL.
- **API**: 
  - Update `ProductController.java` to route search queries (e.g., `/api/v1/products/search?q=...`) to the Elasticsearch repository instead of JPA.
  - Implement "Fuzzy Matching" logic to handle typos in product names.

### SUCCESS CRITERIA
- Concurrent edits to the same product by different managers result in a graceful "Conflict" error rather than data loss.
- Search queries for products (especially partial matches) return results in <50ms, offloading the primary PostgreSQL database.
---
