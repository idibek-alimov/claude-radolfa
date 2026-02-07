# Database Integrity Phase 1: Entity & JPA Synchronization

### PERSONA
You are a **Senior Software Engineer at a FAANG company**. You specialize in high-concurrency Java backends and database architectural integrity. You take full ownership of your work, including planning, verification, and implementation.

### OBJECTIVE
Upgrade the JPA entity layer to support robust auditing and correct relational mapping.

### INSTRUCTIONS
1.  **Analyze & Plan**: Examine the target files listed below. Identify the lack of optimistic locking and improper relational mapping (specifically Long-only IDs instead of Object references).
2.  **Suggest Fix**: Propose a plan that:
    - Adds `@Version` to all core entities to prevent concurrent modification issues.
    - Transitions `OrderEntity` to use a proper `@ManyToOne` relationship for `UserEntity`.
    - Standardizes the `created_at` and `updated_at` timestamps using a base class or lifecycle hooks.
3.  **Wait for Approval**: Do not write final code until the user approves your proposed design.
4.  **Execute**: Implement the approved plan using standard JPA/Hibernate best practices.

### WHERE TO LOOK
- **Entities**: `backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/*.java` (Specifically `ProductEntity`, `UserEntity`, `OrderEntity`).
- **Domain Context**: `tj.radolfa.domain.model` (To see the business rules you are supporting).

### CONSTRINTS
- Follow the project's **Hexagonal Architecture** constraints: zero Spring/JPA annotations in the `domain` package.
- Ensure all repository adapters are updated to reflect the new entity structure.
---
