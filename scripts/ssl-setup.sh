#!/usr/bin/env bash
# =============================================================================
# Radolfa — SSL Certificate Setup (Let's Encrypt)
# =============================================================================
# Run ONCE after bootstrap-vps.sh and BEFORE starting the full docker stack.
# All 3 domains must have DNS A records pointing to this VPS before running.
#
# Certbot method: --standalone (binds to port 80 directly)
# Port 80 must be free — this script stops the nginx container if it's running.
#
# After this script, /etc/letsencrypt/live/* will contain certs for:
#   radolfa.site (+ www.radolfa.site)
#   api.radolfa.site
#   erp.radolfa.site
#
# Renewal: a cron job in infra/cron/certbot-renew handles auto-renewal.
# The cron stops nginx, renews, then starts nginx again (<30 s downtime).
#
# Usage:
#   sudo bash scripts/ssl-setup.sh
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration — edit before running
# ---------------------------------------------------------------------------
CERTBOT_EMAIL="alikcey.2001@gmail.com"

DOMAINS=(
    "radolfa.site"
    "api.radolfa.site"
    "erp.radolfa.site"
)

WILDCARD_DOMAINS_RADOLFA="radolfa.site,www.radolfa.site"
APP_DIR="/opt/radolfa"
COMPOSE_FILE="$APP_DIR/docker-compose.prod.yml"
ENV_FILE="$APP_DIR/.env.production"
# ---------------------------------------------------------------------------

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

[[ $EUID -ne 0 ]] && error "Run as root: sudo bash scripts/ssl-setup.sh"

info "=== Checking DNS resolution ==="
for domain in radolfa.site api.radolfa.site erp.radolfa.site www.radolfa.site; do
    if ! host "$domain" &>/dev/null; then
        warn "DNS lookup failed for $domain — make sure A records point to this VPS before continuing."
    else
        info "  $domain → $(dig +short A $domain | head -1)"
    fi
done
echo ""
read -rp "DNS looks correct? Press ENTER to continue, Ctrl+C to abort."

info "=== Stopping nginx container (need port 80 free for certbot) ==="
if docker ps --format '{{.Names}}' | grep -q radolfa-nginx; then
    docker stop radolfa-nginx
    info "nginx container stopped."
else
    info "nginx not running — proceeding."
fi

# Ensure nothing else holds port 80
if ss -tlnp | grep -q ':80 '; then
    error "Port 80 is still in use by another process. Free it first."
fi

info "=== Ensuring certbot is installed ==="
if ! command -v certbot &>/dev/null; then
    apt-get install -y certbot
fi

mkdir -p /var/www/certbot

info "=== Obtaining certificate for radolfa.site + www.radolfa.site ==="
certbot certonly \
    --standalone \
    --non-interactive \
    --agree-tos \
    --email "$CERTBOT_EMAIL" \
    -d radolfa.site \
    -d www.radolfa.site

info "=== Obtaining certificate for api.radolfa.site ==="
certbot certonly \
    --standalone \
    --non-interactive \
    --agree-tos \
    --email "$CERTBOT_EMAIL" \
    -d api.radolfa.site

info "=== Obtaining certificate for erp.radolfa.site ==="
certbot certonly \
    --standalone \
    --non-interactive \
    --agree-tos \
    --email "$CERTBOT_EMAIL" \
    -d erp.radolfa.site

info "=== Certificate listing ==="
certbot certificates

info "=== Starting application stack ==="
if [[ -f "$COMPOSE_FILE" && -f "$ENV_FILE" ]]; then
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
    info "Stack started."
else
    warn "docker-compose.prod.yml or .env.production not found at $APP_DIR."
    warn "Run scripts/deploy.sh after placing those files."
fi

echo ""
info "================================================================"
info "SSL setup complete!"
info "================================================================"
info "Certificates are in /etc/letsencrypt/live/"
info ""
info "Auto-renewal is handled by the cron job in infra/cron/certbot-renew."
info "Copy it to /etc/cron.d/certbot-renew to activate:"
info "  sudo cp infra/cron/certbot-renew /etc/cron.d/certbot-renew"
info "  sudo chmod 644 /etc/cron.d/certbot-renew"
