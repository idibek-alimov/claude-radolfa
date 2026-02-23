#!/usr/bin/env bash
# =============================================================================
# Radolfa — ERPNext v15 + POSAwesome Installation Script
# =============================================================================
# Run as the 'deploy' user (NOT root) after bootstrap-vps.sh completes.
#
# What this script does:
#   1. Installs all system prerequisites for frappe-bench
#   2. Installs Node.js 20 LTS (required by POSAwesome — minimum 20.8.1)
#   3. Installs MariaDB 10.6 and configures charset for Frappe
#   4. Installs frappe-bench via pip
#   5. Initialises bench at /opt/frappe/frappe-bench
#   6. Fetches ERPNext v15
#   7. Fetches POSAwesome (defendicon/POS-Awesome-V15 — the maintained v15 fork)
#   8. Creates site: erp.radolfa.site with ERPNext + POSAwesome
#   9. Sets up Supervisor (NOT nginx — nginx is managed by docker-compose)
#  10. Configures gunicorn workers (2 — tuned for 4 GB VPS)
#  11. Enables the scheduler and sets maintenance mode off
#
# After this script, gunicorn will listen on 127.0.0.1:8000
# and socketio will listen on 127.0.0.1:9000
#
# Usage:
#   bash scripts/install-erpnext.sh
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration — edit before running
# ---------------------------------------------------------------------------
BENCH_DIR="/opt/frappe/frappe-bench"
SITE_NAME="erp.radolfa.site"
FRAPPE_BRANCH="version-15"

# Passwords — CHANGE THESE before running
MARIADB_ROOT_PASSWORD="Alimov.2001"
SITE_ADMIN_PASSWORD="Oisha.2024"

# Workers tuned for 4 GB VPS test deployment
GUNICORN_WORKERS=2
# ---------------------------------------------------------------------------

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

[[ $EUID -eq 0 ]] && error "Do NOT run as root. Run as the 'deploy' user."
[[ "$MARIADB_ROOT_PASSWORD" == "CHANGE_ME_mariadb_root_password" ]] && error "Set MARIADB_ROOT_PASSWORD before running."
[[ "$SITE_ADMIN_PASSWORD" == "CHANGE_ME_erpnext_admin_password" ]]  && error "Set SITE_ADMIN_PASSWORD before running."

info "=== Step 1: System prerequisites ==="
sudo apt-get update -q
sudo apt-get install -y -q \
    git python3-dev python3-pip python3-setuptools python3-venv \
    libmysqlclient-dev libffi-dev libssl-dev \
    redis-server wkhtmltopdf xvfb libxrender1 libfontconfig1 \
    build-essential supervisor

info "=== Step 2: Node.js 20 LTS (minimum required by POSAwesome) ==="
NODE_MAJOR=$(node --version 2>/dev/null | grep -oP '(?<=^v)\d+' || echo "0")
if [[ "$NODE_MAJOR" -ge 20 ]]; then
    warn "Node.js $(node --version) already installed — meets requirement (>=20)."
else
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt-get install -y nodejs
fi
info "Node: $(node --version)  npm: $(npm --version)"

info "=== Step 3: Yarn ==="
sudo npm install -g yarn
info "Yarn: $(yarn --version)"

info "=== Step 4: MariaDB ==="
if ! command -v mysql &>/dev/null; then
    sudo apt-get install -y mariadb-server mariadb-client
fi

# Configure charset and settings required by Frappe
sudo tee /etc/mysql/mariadb.conf.d/99-frappe.cnf > /dev/null <<'EOF'
[mysqld]
character-set-client-handshake = FALSE
character-set-server            = utf8mb4
collation-server                = utf8mb4_unicode_ci
innodb_buffer_pool_size         = 256M
innodb_file_per_table           = 1
max_allowed_packet              = 256M

[mysql]
default-character-set = utf8mb4
EOF

sudo systemctl enable --now mariadb
info "MariaDB configured."

# Secure MariaDB (set root password + remove anon users)
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '${MARIADB_ROOT_PASSWORD}';" || \
    warn "Could not set MariaDB root password — may already be set."
sudo mysql -u root -p"${MARIADB_ROOT_PASSWORD}" <<SQL
DELETE FROM mysql.user WHERE User='';
DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost','127.0.0.1','::1');
DROP DATABASE IF EXISTS test;
DELETE FROM mysql.db WHERE Db='test' OR Db='test\\_%';
FLUSH PRIVILEGES;
SQL
info "MariaDB secured."

info "=== Step 5: Redis ==="
sudo systemctl enable --now redis-server
info "Redis running."

info "=== Step 6: frappe-bench ==="
pip3 install --quiet frappe-bench
export PATH="$HOME/.local/bin:$PATH"
# Persist PATH for interactive sessions so 'bench' works after login
if ! grep -q '\.local/bin' ~/.bashrc; then
    echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc
fi
info "frappe-bench: $(bench --version)"

info "=== Step 7: Init bench at $BENCH_DIR ==="
sudo mkdir -p /opt/frappe
sudo chown deploy:deploy /opt/frappe

if [[ -d "$BENCH_DIR" ]]; then
    warn "Bench directory already exists at $BENCH_DIR — skipping init."
else
    bench init \
        --frappe-branch "$FRAPPE_BRANCH" \
        --verbose \
        "$BENCH_DIR"
    info "Bench initialised."
fi

cd "$BENCH_DIR"

info "=== Step 7.5: Point bench at system Redis (port 6379) ==="
# bench init creates Redis configs for ports 13000/11000/12000, but those servers
# only start via 'bench start' / Supervisor — they are NOT running during new-site.
# Using the system Redis (port 6379, already started in Step 5) avoids the
# TypeError: int() cannot be 'list' crash that happens when bench tries to reach
# an empty/missing Redis port during site creation.
python3 <<'PYEOF'
import json
path = "sites/common_site_config.json"
with open(path) as f:
    cfg = json.load(f)
for key in ("redis_cache", "redis_queue", "redis_socketio"):
    cfg[key] = "redis://127.0.0.1:6379"
with open(path, "w") as f:
    json.dump(cfg, f, indent=2)
print("common_site_config.json updated — all Redis → 127.0.0.1:6379")
PYEOF
info "Bench Redis config updated."

info "=== Step 8: Get ERPNext v15 ==="
if [[ -d "apps/erpnext" ]]; then
    warn "ERPNext app already present — skipping."
else
    bench get-app --branch "$FRAPPE_BRANCH" erpnext
    info "ERPNext fetched."
fi

info "=== Step 8.5: Get POSAwesome (defendicon/POS-Awesome-V15) ==="
# This is the actively maintained v15 fork — last release Feb 2026.
# Requires Node.js >= 20.8.1 (already satisfied above).
if [[ -d "apps/posawesome" ]]; then
    warn "POSAwesome already present — skipping."
else
    bench get-app https://github.com/defendicon/POS-Awesome-V15
    info "POSAwesome fetched."
fi

info "=== Step 9: Create site: $SITE_NAME ==="
if [[ -d "sites/$SITE_NAME" ]]; then
    warn "Site $SITE_NAME already exists — skipping."
else
    bench new-site "$SITE_NAME" \
        --mariadb-root-password "$MARIADB_ROOT_PASSWORD" \
        --admin-password       "$SITE_ADMIN_PASSWORD" \
        --install-app          erpnext
    info "Site $SITE_NAME created with ERPNext."
fi

info "=== Step 9.5: Install POSAwesome on site ==="
if [[ -d "sites/$SITE_NAME" ]] && bench --site "$SITE_NAME" list-apps 2>/dev/null | grep -q posawesome; then
    warn "POSAwesome already installed on $SITE_NAME — skipping."
else
    bench --site "$SITE_NAME" install-app posawesome
    bench build --app posawesome
    bench --site "$SITE_NAME" migrate
    info "POSAwesome installed and assets built."
fi

info "=== Step 10: Production configuration ==="
# Set current site
bench use "$SITE_NAME"

# Disable developer mode
bench --site "$SITE_NAME" set-config developer_mode 0
bench --site "$SITE_NAME" set-maintenance-mode off
bench --site "$SITE_NAME" enable-scheduler

# Set site URL (important for links in emails and ERPNext internal URLs)
bench --site "$SITE_NAME" set-config hostname "erp.radolfa.site"

info "=== Step 11: Gunicorn workers (tuned for 4 GB VPS) ==="
# bench config sub-commands vary by bench version — set directly in JSON instead
python3 <<PYEOF
import json
path = "sites/common_site_config.json"
with open(path) as f:
    cfg = json.load(f)
cfg["gunicorn_workers"] = ${GUNICORN_WORKERS}
cfg["max_requests"] = 1000
with open(path, "w") as f:
    json.dump(cfg, f, indent=2)
print("gunicorn_workers=${GUNICORN_WORKERS}, max_requests=1000 written to common_site_config.json")
PYEOF

info "=== Step 12: Supervisor setup (NOT nginx — we use our own) ==="
# Generate supervisor config without touching nginx
bench setup supervisor --skip-redis --yes

sudo cp "$BENCH_DIR/config/supervisor.conf" /etc/supervisor/conf.d/frappe-bench.conf
sudo systemctl enable --now supervisor
sudo supervisorctl reread
sudo supervisorctl update
sudo supervisorctl start all
info "Supervisor configured. ERPNext workers started."

info "=== Step 13: Verify gunicorn is listening on port 8000 ==="
sleep 5
if ss -tlnp | grep ':8000'; then
    info "Gunicorn is UP on port 8000."
else
    warn "Port 8000 not detected yet — may still be starting. Check: bench status"
fi

echo ""
info "================================================================"
info "ERPNext v15 + POSAwesome installation complete!"
info "================================================================"
info "  Site name    : $SITE_NAME"
info "  Bench dir    : $BENCH_DIR"
info "  Gunicorn     : localhost:8000"
info "  Socket.io    : localhost:9000"
info "  Admin URL    : https://erp.radolfa.site  (after SSL + nginx deploy)"
info "  POSAwesome   : https://erp.radolfa.site/posawesome  (after login)"
info "  Node.js      : $(node --version)"
echo ""
info "IMPORTANT — nginx is NOT managed by bench."
info "Our Docker nginx (docker-compose.prod.yml) handles SSL + routing."
info "Do NOT run 'bench setup nginx'."
echo ""
info "To check status : cd $BENCH_DIR && bench status"
info "To view logs    : cd $BENCH_DIR && bench logs"
info "To list apps    : bench --site $SITE_NAME list-apps"
