#!/usr/bin/env bash
# =============================================================================
# Radolfa — Database Restore Script
# =============================================================================
# Restores a PostgreSQL backup created by backup.sh.
#
# WARNING: This OVERWRITES the current production database.
# The backend container is stopped during restore to prevent writes.
#
# Usage:
#   bash scripts/restore.sh /opt/radolfa/backups/postgres_20260101_020000.sql.gz
# =============================================================================

set -euo pipefail

BACKUP_FILE="${1:-}"
APP_DIR="/opt/radolfa"
COMPOSE_FILE="$APP_DIR/docker-compose.prod.yml"
ENV_FILE="$APP_DIR/.env.production"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

[[ -z "$BACKUP_FILE" ]]    && error "Usage: restore.sh <backup_file.sql.gz>"
[[ ! -f "$BACKUP_FILE" ]]  && error "Backup file not found: $BACKUP_FILE"
[[ ! -f "$ENV_FILE" ]]     && error "Not found: $ENV_FILE"

# Load DB credentials
# shellcheck disable=SC1090
source <(grep -E '^(POSTGRES_DB|POSTGRES_USER|POSTGRES_PASSWORD)=' "$ENV_FILE")

echo ""
warn "================================================================"
warn "DANGER: This will OVERWRITE the production database."
warn "Backup file : $BACKUP_FILE"
warn "Target DB   : $POSTGRES_DB"
warn "================================================================"
echo ""
read -rp "Type 'yes' to continue: "
[[ "$REPLY" != "yes" ]] && { info "Aborted."; exit 0; }

info "=== Stopping backend container (halt writes) ==="
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" stop backend
info "Backend stopped."

info "=== Dropping and recreating database ==="
docker exec radolfa-db psql -U "$POSTGRES_USER" -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${POSTGRES_DB}' AND pid <> pg_backend_pid();" \
    postgres

docker exec radolfa-db psql -U "$POSTGRES_USER" \
    -c "DROP DATABASE IF EXISTS \"${POSTGRES_DB}\";" postgres

docker exec radolfa-db psql -U "$POSTGRES_USER" \
    -c "CREATE DATABASE \"${POSTGRES_DB}\" OWNER \"${POSTGRES_USER}\";" postgres

info "=== Restoring from $BACKUP_FILE ==="
gunzip -c "$BACKUP_FILE" \
    | docker exec -i radolfa-db psql -U "$POSTGRES_USER" "$POSTGRES_DB"
info "Restore complete."

info "=== Starting backend container ==="
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" start backend
info "Backend started. Waiting 30 s for it to become healthy..."
sleep 30

info "=== Final status ==="
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps

info "Restore finished successfully."
