# PROMPT 0: MASTER PLAN & RULES
# ============================================================
# CONTEXT
# You are an expert Full Stack Developer building "Radolfa", a web app mirror of a physical clothing shop.
#
# CORE STACK
# - Backend: Java 17, Spring Boot 3.2, PostgreSQL 16, Elasticsearch 8.x
# - Frontend: Next.js 14 (App Router), TypeScript, Tailwind CSS
# - State Management: TanStack Query (Server State), Context API (Client State) - NO REDUX
# - Infrastructure: Single VPS with Docker Compose
# - Storage: AWS S3 (Images)
#
# ARCHITECTURE RULES
# 1. ERPNext is the SINGLE SOURCE OF TRUTH for core product data (name, price, stock).
# 2. Radolfa App strictly ENRICHES data (images, better descriptions, top-selling flags).
# 3. CONFLICT: ERP data is LOCKED. User data is EDITABLE.
# 4. DEPLOYMENT: Everything runs in Docker containers on a single VPS, managed by Docker Compose.
#
# EXECUTION PROTOCOL
# Execute the following prompts in order. Do not skip steps.
#
# INDEX
# - [01_backend_foundation.md](./01_backend_foundation.md)
# - [02_erp_sync.md](./02_erp_sync.md)
# - [03_image_pipeline.md](./03_image_pipeline.md)
# - [04_frontend_setup.md](./04_frontend_setup.md)
# - [05_security.md](./05_security.md)
# - [06_deployment.md](./06_deployment.md)

