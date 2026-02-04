# Claude Execution Plan — Radolfa (v4)

**Deployment Target:** Single VPS (Docker Compose)
**Frontend:** Next.js 14 + TanStack Query
**Backend:** Spring Boot Monolith + PostgreSQL + Elasticsearch

---

## 📂 Prompt Execution Order

To build this project efficiently, please execute the **Prompt Files** in the `prompts/` directory in the following order. Each file contains specific instructions for a distinct phase of development.

### 1. [00_master_plan.md](./prompts/00_master_plan.md)
*Overview of the architecture and global rules.*

### 2. [01_backend_foundation.md](./prompts/01_backend_foundation.md)
*Spring Boot setup, Database config, and Domain Entities.*

### 3. [02_erp_sync.md](./prompts/02_erp_sync.md)
*Logic for syncing locked data from ERPNext.*

### 4. [03_image_pipeline.md](./prompts/03_image_pipeline.md)
*S3 integration and ImageService implementation.*

### 5. [04_frontend_setup.md](./prompts/04_frontend_setup.md)
*Next.js 14 setup, TanStack Query, and core UI components.*

### 6. [05_security.md](./prompts/05_security.md)
*Phone OTP Auth and JWT Security config.*

### 7. [06_deployment.md](./prompts/06_deployment.md)
*Dockerfiles, Docker Compose, and Nginx reverse proxy setup.*
