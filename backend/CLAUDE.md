# Radolfa Backend — Hexagonal Guardrails
- **Pattern:** Strict Hexagonal (Ports & Adapters).
- **Domain:** `tj.radolfa.domain` (Zero dependencies — no Spring, no JPA, no Jackson).
- **Application:** `tj.radolfa.application` (Use Cases & Ports).
- **Infrastructure:** `tj.radolfa.infrastructure` (Adapters: JPA, REST, S3).
- **Rules:**
  - All mapping between layers MUST use MapStruct (or explicit `default` mapper methods for complex domain types).
  - No JPA `@Entity` annotations in the Domain layer.
  - Use Java 21 `record` for DTOs. Domain models may be mutable classes when they have business behaviour (e.g. `Cart`, `Sku`).
  - Constructor injection only — no `@Autowired` on fields.
  - `@Transactional` belongs on the service layer, not on adapters (except `SaveCartPort`, `SaveProductHierarchyPort` which are called from multiple services).
  - **List endpoints:** Every endpoint returning a list MUST accept `page`, `size`, `search`, and `sort` as query params and apply them inside the JPA Specification / Elasticsearch query — never return an unfiltered `List<T>` and let the caller trim it. Return Spring `Page<T>` (fields: `content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`).
  - **Search:** Apply case-insensitive `LIKE` / Elasticsearch `match` on the fields the UI exposes as searchable. Add a Flyway migration for a DB index if the column is not already indexed.
  - **Sort:** Accept `sort=field,asc|desc` (Spring `Sort` convention). **Whitelist** sortable field names in the controller — never interpolate raw user input into JPQL or native SQL `ORDER BY` (SQL injection risk).
  - **Why:** The frontend is forbidden from filtering `content[]` locally (see root `CLAUDE.md` and `frontend/CLAUDE.md`). If the backend does not implement search/sort, the feature is broken, not degraded.

## Database Migrations — Development Policy

**Rule: No `ALTER TABLE` in dev. Edit the original `CREATE TABLE` instead.**

Before writing any `ALTER TABLE` migration, ask: *"Is this a development codebase with no production data to preserve?"* If yes, find the original `VN__*.sql` file that first created the table and add the column/index/constraint there directly. Then delete (or don't create) the `ALTER TABLE` file.

**Why:** `ALTER TABLE` files accumulate fast. In dev you wipe and re-seed the database anyway, so incremental patches just add noise and slow down reasoning about the schema.

**When `ALTER TABLE` IS acceptable:**
- The project is deployed to production and has live data that cannot be recreated.
- The change must be applied without dropping the table (e.g., zero-downtime migration on a prod system).

**How to apply the rule:**
1. Find the original migration file (e.g., `V7__reviews_and_qa.sql` for the `reviews` table).
2. Add the new column/index inside the `CREATE TABLE` block in that file.
3. Do NOT create a new `VN+1__add_column_to_X.sql` file.
4. If the migration file that would have been an `ALTER TABLE` also creates *new* tables, keep only the `CREATE TABLE` parts and remove the `ALTER TABLE` line.
