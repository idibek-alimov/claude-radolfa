---
name: migration-gen
description: Generate the next Flyway migration SQL file with correct versioning for the Radolfa project.
---

You are generating a Flyway migration for the Radolfa project (PostgreSQL 16).

## Instructions

1. List existing migration files in `backend/src/main/resources/db/migration/` to find the current highest V number.
2. Increment that number by 1.
3. Create the file at `backend/src/main/resources/db/migration/V{n}__{description}.sql` where `{description}` is the argument provided by the user in `snake_case` (all lowercase, underscores only).
4. Write idempotent PostgreSQL SQL for the change described. Follow these conventions:
   - Use `IF NOT EXISTS` / `IF EXISTS` guards where applicable.
   - Use `BIGSERIAL` for surrogate PKs, `UUID` for external-facing IDs.
   - Foreign keys must have explicit `ON DELETE` clauses.
   - Never use `DROP` on existing production tables without explicit instruction.
   - Add an index for every foreign key column.
5. Output the full file path and SQL content, then ask for confirmation before writing.

## Example

Invoked as: `/migration-gen add_product_reviews`

Result: creates `V15__add_product_reviews.sql` with a `CREATE TABLE product_reviews (...)` statement.
