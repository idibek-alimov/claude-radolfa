# Engineering Audit: Database Migrations & Schema Integrity

**Author**: Senior Fullstack Engineer (15+ YOE)  
**Objective**: Identify structural risks in the Flyway migration logic and propose FAANG-grade remediation.

---

## 1. Identified Risks & "Manual" Alteration Pitfalls

### A. DML Mixed with DDL (Critical Scalability Risk)
*   **Location**: `V5__enhance_user_table.sql`
*   **Observation**: The migration performs an `UPDATE users SET updated_at = ...` immediately after adding the column.
*   **FAANG Perspective**: For a table with 10M+ users, this `UPDATE` will lock the table for minutes/hours, causing a production outage (Exclusive Lock).
*   **Issue**: Mixing schema changes with data backfills is an anti-pattern. If the data update fails, the whole migration rolls back, but the DB might be left in a partially locked state.
*   **Fix**: Separate migrations into "Schema Only" (DDL) and "Data Backfill" (DML). Use a background Job (e.g., Spring Batch) to backfill data in chunks rather than a single SQL statement.

### B. Brittle Check Constraints (Flexibility Risk)
*   **Location**: `V1__init.sql` (Role Check)
*   **Observation**: `CHECK (role IN ('USER', 'MANAGER', 'SYSTEM'))`
*   **Issue**: Database-level `CHECK` constraints for application-level enums are brittle. If the business decides to add an `ADMIN` or `SUPPORT` role, the backend code change is trivial, but the database will reject the new values until a new migration is run to drop and recreate the constraint.
*   **Fix**: Move role management to a `roles` table with a Foreign Key, or rely on application-level validation if the set of roles is highly dynamic. Avoid hardcoded `CHECK` strings in DDL.

### C. The Array Anti-Pattern (Query Performance)
*   **Location**: `V1__init.sql` / `ProductEntity.java` (`images TEXT[]`)
*   **Observation**: Images are stored as a Postgres Array.
*   **Issue**: 
    1.  **Standardization**: Arrays are not standard SQL, making database migration (e.g., to Aurora/RDS MySQL) difficult.
    2.  **Indexing**: Individual images cannot be efficiently indexed without specialized `GIN` indexes.
    3.  **Audit**: You cannot easily track when a specific image was added or who added it without parsing the whole array.
*   **Fix**: Create a normalized `product_images` table (`id`, `product_id`, `url`, `is_primary`, `created_at`). This allows for metadata per image and standard SQL querying.

### D. Denormalization "Silent Failures"
*   **Location**: `V6__create_orders_table.sql` (`product_name` in `order_items`)
*   **Observation**: The item table stores both `product_id` (FK) and `product_name` (String).
*   **Issue**: This is a "Historical Snapshot." While useful, it becomes a problem if the application logic doesn't explicitly handle *why* it's there. If a manager fixes a typo in a product name, should the order history reflect the fix? 
*   **Risk**: If the `product_name` isn't populated correctly during the order creation transaction, the data is lost forever.
*   **Fix**: Define a clear "Domain Snapshots" pattern. Ensure the persistence layer explicitly maps these "snapshot" fields during the order placement transition.

### E. Destructive Deletion Policy
*   **Location**: `DeleteProductPort` and `V6` Foreign Keys.
*   **Observation**: The system allows hard `DELETE` of products.
*   **Issue**: Hard deleting a product that has existing `order_items` will fail (FK constraint) or result in "orphaned" history if not handled.
*   **FAANG Perspective**: We never delete. We **ARCHIVE**.
*   **Fix**: Implement **Soft Delete** (`deleted_at` column) across all core entities. Update the JPA entities with `@Where(clause = "deleted_at IS NULL")`.

---

## 2. Professional Remediation Roadmap

### Phase 1: Entity & JPA Synchronization
1.  **Versioned Auditing**: Add `@Version` to all JPA entities to prevent the "Lost Update" problem in concurrent manager sessions.
2.  **User Entity Refinement**: Replace `private Long userId` in `OrderEntity` with a proper `@ManyToOne` relationship to enable lazy-loading and object graph traversal.

### Phase 2: Migration Strategy Upgrade
1.  **Backfill Separation**: Create a `V8__fix_users_updated_at.sql` (or similar) using a safe, non-locking update strategy if needed, but primarily move to worker-based backfills for data changes.
2.  **Image Normalization**: Prepare a migration to move `products.images` (array) to a `product_images` table. This is a standard "breaking" change that requires a two-step deploy (Dual-Write -> Migrate -> Read from New).

### Phase 3: Defensive Constraints
1.  **Safe Enums**: Transition `CHECK` constraints to a lookup table for Roles and Order Statuses.
2.  **Audit Columns**: Standardize `created_at` and `updated_at` across **all** tables via a base JPA class (`MappedSuperclass`).

---

## Summary
The current migrations are "functional" but lack the safeguards required for a high-concurrency, high-growth environment. By implementing **Normalized Media**, **Soft Deletes**, and **Safe Backfill Patterns**, you will prevent future production "hair-on-fire" scenarios.
