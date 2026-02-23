#!/usr/bin/env bash
# =============================================================================
# Radolfa — Backup Script
# =============================================================================
# Backs up:
#   1. PostgreSQL database (via pg_dump inside the container)
#   2. Elasticsearch data directory (volume tarball)
#
# Retention: keeps last 7 daily backups.
# Location: /opt/radolfa/backups/
#
# Cron: see infra/cron/backup (runs daily at 02:30)
#
# Usage:
#   bash scripts/backup.sh
# =============================================================================

set -euo pipefail

APP_DIR="/opt/radolfa"
BACKUP_DIR="$APP_DIR/backups"
ENV_FILE="$APP_DIR/.env.production"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=7
LOG_FILE="/var/log/radolfa/backup.log"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $*" | tee -a "$LOG_FILE"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*" | tee -a "$LOG_FILE"; }
error() { echo -e "${RED}[ERROR]${NC} $*" | tee -a "$LOG_FILE"; exit 1; }

[[ ! -f "$ENV_FILE" ]] && error "Not found: $ENV_FILE"

# Load production environment variables
# shellcheck disable=SC1090
source <(grep -E '^(POSTGRES_DB|POSTGRES_USER|POSTGRES_PASSWORD)=' "$ENV_FILE")

mkdir -p "$BACKUP_DIR"

info "=== Backup started: $TIMESTAMP ==="

# ---------------------------------------------------------------------------
# 1. PostgreSQL
# ---------------------------------------------------------------------------
PG_BACKUP="$BACKUP_DIR/postgres_${TIMESTAMP}.sql.gz"

info "Dumping PostgreSQL: $POSTGRES_DB → $(basename "$PG_BACKUP")"

if ! docker ps --format '{{.Names}}' | grep -q 'radolfa-db'; then
    error "radolfa-db container is not running — cannot backup."
fi

docker exec radolfa-db \
    pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" \
    | gzip > "$PG_BACKUP"

PG_SIZE=$(du -sh "$PG_BACKUP" | cut -f1)
info "PostgreSQL backup complete: $PG_SIZE — $(basename "$PG_BACKUP")"

# ---------------------------------------------------------------------------
# 2. Prune old backups
# ---------------------------------------------------------------------------
info "Pruning backups older than $RETENTION_DAYS days..."
find "$BACKUP_DIR" -name "postgres_*.sql.gz" -mtime +"$RETENTION_DAYS" -delete
REMAINING=$(find "$BACKUP_DIR" -name "postgres_*.sql.gz" | wc -l)
info "Retained backups: $REMAINING"

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
info "=== Backup complete: $TIMESTAMP ==="
info "Location: $BACKUP_DIR"
ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null || true
