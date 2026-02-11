# Claude Code Prompt: Legacy Product Clean Slate

To remove the legacy "Product" system remnants and reset the SQL schema for the 3-tier hierarchy, use the following prompt:

---

### Prompt for Claude Code

**Objective**: Completely purge the legacy "Product" system and reset the database migration history to align with the current 3-tier product hierarchy (`ProductBase` → `ListingVariant` → `Sku`).

**Directives**:

1.  **Database Migration Reset**:
    *   **Delete all** existing SQL migration files in `backend/src/main/resources/db/migration/`.
    *   **Generate** a new, clean set of migration files (starting from `V1`) that initializes the current target schema (Users, ProductBase, ListingVariant, Sku, Orders, etc.) based on the latest domain models and entities.
    *   Ensure the new migrations are clean and do not contain any legacy "products" table logic.

2.  **Autonomous Code Cleanup**:
    *   Analyze the backend Java source code to identify all remnants of the legacy `Product` model and its associated (unimplemented/obsolete) ports and services.
    *   Do NOT just delete files; think through the dependencies in `SecurityConfig`, `OpenApiConfig`, and other infrastructure layers.
    *   Develop a comprehensive plan to remove these remnants while preserving the new 3-tier hierarchy logic.

3.  **Scope for Analysis**:
    To optimize token usage, focus your initial analysis on these directories:
    *   `backend/src/main/java/tj/radolfa/domain/model/`
    *   `backend/src/main/java/tj/radolfa/application/ports/`
    *   `backend/src/main/java/tj/radolfa/application/services/`
    *   `backend/src/main/java/tj/radolfa/infrastructure/persistence/`
    *   `backend/src/main/java/tj/radolfa/infrastructure/web/`

**Workflow**:
1.  **Plan**: Draw an implementation plan covering both the SQL reset and the Java code cleanup.
2.  **Confirm**: Wait for my approval of the plan.
3.  **Execute**: Implement all deletions and modifications.
4.  **Verify**: Ensure the backend compiles successfully (`./mvnw clean compile`).

---
