# Database Integrity Phase 3: Defensive Constraints

### PERSONA
You are a **Senior Software Engineer at a FAANG company**. You value defensive programming and high-integrity schemas that prevent bad data from ever entering the system.

### OBJECTIVE
Implement defensive schema constraints and archival policies to ensure long-term data safety.

### INSTRUCTIONS
1.  **Analyze & Plan**: Examine the current `CHECK` constraints and the lack of a deletion policy.
2.  **Suggest Fix**: Propose a plan that:
    - Moves dynamic Enums (Roles, Statuses) to **Lookup Tables** with Foreign Keys to avoid brittle `CHECK` constraints.
    - Implements a **Soft Delete** strategy (e.g., `deleted_at`) for Products and Orders to prevent breaking historical records.
    - Standardizes audit column triggers or JPA listeners across the entire schema.
3.  **Wait for Approval**: Do not apply changes until the user approves the specific tables and columns added.
4.  **Execute**: Implement the approved schema changes and update the application logic (specifically the "Delete" ports) to perform Soft Deletes.

### WHERE TO LOOK
- **DDL History**: `backend/src/main/resources/db/migration/V1__init.sql` and `V6__create_orders_table.sql`.
- **Delete Logic**: `tj.radolfa.application.ports.out.DeleteProductPort` and its implementation in the persistence layer.

### CONSTRAINTS
- **Backward Compatibility**: Ensure that existing orders and users still function during the transition to lookup tables.
- **JPA Integration**: Use `@Where(clause = "deleted_at IS NULL")` or similar Hibernate filters to make soft-delete transparent to the rest of the application.
---
