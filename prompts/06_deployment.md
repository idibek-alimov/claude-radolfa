# PROMPT 6: VPS DEPLOYMENT
# ============================================================
# GOAL: Deploy everything on a single VPS using Docker Compose.
#
# INSTRUCTIONS
# ------------------------------------------------------------
# 1. Dockerfiles
#    - Backend: Multi-stage build (Maven build -> OpenJDK runtime). Expose 8080.
#    - Frontend: Multi-stage build (Node build -> Node runtime). Expose 3000.
#
# 2. docker-compose.yml
#    - Services:
#      - `db`: PostgreSQL 16 (Volume mapped).
#      - `elasticsearch`: ES 8.x (Volume mapped, limits configured).
#      - `backend`: Depends on db, es. Env vars for DB credentials, AWS keys.
#      - `frontend`: Depends on backend.
#      - `nginx`: Reverse proxy.
#
# 3. Nginx Config
#    - Listen on 80/443.
#    - `/api/` -> Proxy to `backend:8080`.
#    - `/` -> Proxy to `frontend:3000`.
#    - Serve static assets if needed.
#
# 4. Production Readiness
#    - Restart policies: `always`.
#    - Health checks.
#    - Logging drivers.
