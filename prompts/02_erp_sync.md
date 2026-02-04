# PROMPT 2: ERP SYNC & BATCHING
# ============================================================
# GOAL: Sync data from ERPNext to Radolfa.
#
# INSTRUCTIONS
# ------------------------------------------------------------
# 1. Spring Batch Job: `erpInitialImportJob`
#    - Reader: Fetch all items from ERPNext API (paginated).
#    - Processor: Map ERP fields to Product entities.
#      * IMPORTANT: If product exists, UPDATE only locked fields.
#      * IMPORTANT: If product is new, create it.
#    - Writer: Save to PostgreSQL and Index to Elasticsearch.
#
# 2. Incremental Sync API
#    - Endpoint: `POST /api/v1/sync/products`
#    - Security: API Key protected (SYSTEM role only).
#    - Logic: Upsert logic.
#      - Match by `erpId`.
#      - Overwrite `name`, `price`, `stock`.
#      - PRESERVE `webDescription`, `images`.
#
# 3. Eventual Consistency
#    - Scheduled task (e.g., every hour) to run the batch job for full reconciliation.
