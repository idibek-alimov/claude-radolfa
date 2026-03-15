# Backend Docker Startup Error Report

## Error Summary
When running `docker compose up -d`, the `radolfa-backend` container immediately crashes and shuts down, causing it to be marked as unhealthy. 

The root cause is a **Flyway Database Migration Failure** occurring during the Spring Boot application startup sequence.

## Stack Trace & Details

The fatal error from the backend logs is:

```text
Caused by: org.flywaydb.core.internal.sqlscript.FlywaySqlScriptException: Migration V17_1__seed_discount_scenarios.sql failed
---------------------------------------------------
SQL State  : 42703
Error Code : 0
Message    : ERROR: column "discount_percentage" of relation "skus" does not exist
  Position: 1484
Location   : db/migration-dev/V17_1__seed_discount_scenarios.sql (/app/nested:/app/app.jar/!BOOT-INF/classes/!/db/migration-dev/V17_1__seed_discount_scenarios.sql)
Line       : 28
Statement  : 
-- ----------------------------------------------------------------
-- Group 1: Products 1-6 → CLEAR discounted_price (no ERP discount)
-- These test: Case 0 (anonymous) and Case 2 (loyalty-only for logged-in)
-- ----------------------------------------------------------------
UPDATE skus SET discounted_price = NULL, discounted_ends_at = NULL, discount_percentage = NULL
WHERE listing_variant_id IN (
    SELECT lv.id FROM listing_variants lv
    JOIN product_bases pb ON lv.product_base_id = pb.id
    WHERE pb.erp_template_code IN (
        'TPL-TSHIRT-001', 'TPL-HOODIE-001', 'TPL-JEANS-001',
        'TPL-WINDBRK-001', 'TPL-SKIRT-001', 'TPL-DRESS-001'
    )
)
```

## Diagnosis
The dev-only seed script (`V17_1__seed_discount_scenarios.sql`) is attempting to set the `discount_percentage` column on the `skus` table to `NULL`.

However, the `discount_percentage` column **does not exist** in the `skus` table in the database schema. This indicates a mismatch between the Flyway schema definitions (likely missing an `ALTER TABLE` to add the column, or it was removed in a previous migration) and what the seed data script expects to exist.

Because Flyway fails to run this script, Spring Boot terminates the application context and the container exits with Code 1.
