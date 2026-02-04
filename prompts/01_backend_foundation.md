# PROMPT 1: BACKEND FOUNDATION
# ============================================================
# GOAL: Initialize the Spring Boot project and core domain models.
#
# INSTRUCTIONS
# ------------------------------------------------------------
# 1. Initialize Spring Boot 3.2 Project (Java 17, Maven)
#    - Dependencies: Web, Data JPA, Security, Validation, Actuator, Batch, Data Elasticsearch, PostgreSQL, AWS SDK v2, Thumbnailator, Lombok.
#
# 2. Database Configuration
#    - Configure PostgreSQL 16 connection in `application.yml`.
#    - Use `update` or `validate` for ddl-auto.
#    - Set up Flyway for migrations (V1__init.sql).
#
# 3. Implement Product Entity (Enrichment Pattern)
#    - `id` (Long, PK)
#    - `erpId` (String, unique, indexed) -> Source of truth ID
#    - `name` (String) -> LOCKED (Managed by ERP)
#    - `price` (BigDecimal) -> LOCKED
#    - `stock` (Integer) -> LOCKED
#    - `webDescription` (Text) -> EDITABLE (Enrichment)
#    - `isTopSelling` (Boolean) -> EDITABLE (Enrichment)
#    - `images` (List<String>) -> EDITABLE (S3 URLs)
#
# 4. Implement User Entity
#    - `phone` (String, unique)
#    - `role` (Enum: USER, MANAGER, SYSTEM)
#
# DELIVERABLE
# - Compilable Spring Boot Application
# - Docker Compose snippet for local Postgres/Elasticsearch development
