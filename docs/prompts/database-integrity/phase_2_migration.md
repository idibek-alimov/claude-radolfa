# Database Integrity Phase 2: Migration Strategy Upgrade

### PERSONA
You are a **Senior Software Engineer at a FAANG company**. You are an expert in Zero-Downtime database migrations and high-performance schema design.

### OBJECTIVE
Modernize the database migration strategy to handle large-scale data and normalize media storage.

### INSTRUCTIONS
1.  **Analyze & Plan**: Review the Flyway migrations and identifying the "Array Anti-Pattern" for images and the "DML-in-DDL" risk in `V5`.
2.  **Suggest Fix**: Propose a 2-step migration plan:
    - **Step 1 (Normalization)**: Create a `product_images` table to replace the `TEXT[]` array. Include a plan for data migration (Dual-write or background backfill).
    - **Step 2 (Safety)**: Identify any existing DML (data updates) in DDL scripts and propose a strategy to move them to safe background processes.
3.  **Wait for Approval**: Do not generate SQL or Code until the user approves the strategy.
4.  **Execute**: Create the necessary Flyway migration files (`V8__...`) and update the persistence adapters/entities to support the normalized structure.

### WHERE TO LOOK
- **Migrations**: `backend/src/main/resources/db/migration/*.sql` (Especially `V1`, `V5`, and `V7`).
- **Media Logic**: `tj.radolfa.infrastructure.persistence.entity.ProductEntity` and its corresponding mapper/repository.

### CONSTRAINTS
- **Zero Data Loss**: Ensure the migration from Array to Table preserves all existing seed data URLs.
- **Portability**: Avoid vendor-specific SQL types (like Postgres arrays) where standard relational tables are more performant for indexing.
---
