#!/usr/bin/env bash
# =============================================================================
# Radolfa — Manual Deployment Script
# =============================================================================
# Use this for manual deploys or rollbacks outside of GitHub Actions.
#
# Usage:
#   bash scripts/deploy.sh                   # deploy IMAGE_TAG from .env.production
#   bash scripts/deploy.sh a1b2c3d4          # deploy a specific git SHA tag
#   bash scripts/deploy.sh latest            # pin to :latest
# =============================================================================

set -euo pipefail

APP_DIR="/opt/radolfa"
COMPOSE_FILE="$APP_DIR/docker-compose.prod.yml"
ENV_FILE="$APP_DIR/.env.production"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

[[ ! -f "$COMPOSE_FILE" ]] && error "Not found: $COMPOSE_FILE — copy docker-compose.prod.yml to $APP_DIR first."
[[ ! -f "$ENV_FILE" ]]     && error "Not found: $ENV_FILE — create the production env file first."

# Override IMAGE_TAG if passed as argument
if [[ $# -ge 1 ]]; then
    IMAGE_TAG="$1"
    info "Using IMAGE_TAG: $IMAGE_TAG"
    sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=${IMAGE_TAG}/" "$ENV_FILE"
else
    IMAGE_TAG=$(grep '^IMAGE_TAG=' "$ENV_FILE" | cut -d= -f2)
    info "Using IMAGE_TAG from .env.production: $IMAGE_TAG"
fi

info "=== Logging in to GHCR ==="
# GitHub Actions uses GITHUB_TOKEN. For manual deploys, set GHCR_TOKEN env var.
if [[ -n "${GHCR_TOKEN:-}" ]]; then
    echo "$GHCR_TOKEN" | docker login ghcr.io -u idibek-alimov --password-stdin
    info "Logged in to GHCR."
else
    warn "GHCR_TOKEN not set — assuming already logged in or images are public."
fi

info "=== Pulling images: tag=${IMAGE_TAG} ==="
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull

info "=== Deploying stack ==="
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --remove-orphans

info "=== Waiting 45 s for services to become healthy ==="
sleep 45

info "=== Container status ==="
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps

info "=== Health checks ==="
FAILED=0

check_health() {
    local name="$1"
    local url="$2"
    if curl -sf --max-time 10 "$url" > /dev/null 2>&1; then
        info "  [OK] $name ($url)"
    else
        warn "  [FAIL] $name ($url)"
        FAILED=1
    fi
}

check_health "Nginx"        "http://localhost/nginx-health"
check_health "Backend"      "http://localhost:8080/actuator/health" 2>/dev/null || \
    warn "  Backend health via nginx only (port not exposed externally)"
check_health "ERPNext"      "http://localhost:8000"

if [[ $FAILED -eq 0 ]]; then
    info "All health checks passed. Deploy successful: ${IMAGE_TAG}"
else
    warn "One or more health checks failed. Review logs:"
    warn "  docker compose -f $COMPOSE_FILE logs --tail=50"
fi
