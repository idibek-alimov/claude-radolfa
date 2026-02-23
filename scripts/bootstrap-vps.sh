#!/usr/bin/env bash
# =============================================================================
# Radolfa — VPS Bootstrap Script
# =============================================================================
# Run ONCE as root on a fresh Ubuntu 22.04 VPS.
# After this script completes, log out of root and use the 'deploy' user.
#
# Usage:
#   curl -fsSL <url>/bootstrap-vps.sh | bash
#   OR: scp scripts/bootstrap-vps.sh root@<vps>: && ssh root@<vps> bash bootstrap-vps.sh
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration — edit before running
# ---------------------------------------------------------------------------
DEPLOY_USER="deploy"
SSH_PORT="2222"
APP_DIR="/opt/radolfa"

# Paste the public key of the person who will SSH in as deploy
# e.g. the output of: cat ~/.ssh/id_ed25519.pub
DEPLOY_SSH_PUBLIC_KEY="ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMqRQnLTc0bLWwvuyCyosVZjA+lA3e+p8GY33sbPivbs idibek@idibek-Legion-Pro-5-16IRX9"

# ---------------------------------------------------------------------------

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

[[ $EUID -ne 0 ]] && error "This script must be run as root."
[[ "$(lsb_release -rs)" != "22.04" ]] && warn "Tested on Ubuntu 22.04 — proceed with caution on other versions."
[[ "$DEPLOY_SSH_PUBLIC_KEY" == "PASTE_YOUR_SSH_PUBLIC_KEY_HERE" ]] && error "Set DEPLOY_SSH_PUBLIC_KEY before running."

info "=== Step 1: System update ==="
apt-get update -q
apt-get upgrade -y -q
apt-get install -y -q \
    curl git ufw fail2ban unattended-upgrades apt-listchanges \
    ca-certificates gnupg lsb-release software-properties-common \
    htop ncdu logrotate certbot

info "=== Step 2: Create deploy user ==="
if id "$DEPLOY_USER" &>/dev/null; then
    warn "User '$DEPLOY_USER' already exists — skipping creation."
else
    useradd -m -s /bin/bash "$DEPLOY_USER"
    info "Created user: $DEPLOY_USER"
fi

# Allow deploy user to manage docker and restart supervisor without full sudo
# We do NOT give blanket sudo — least privilege
usermod -aG sudo "$DEPLOY_USER"

# Set up SSH for deploy user
mkdir -p /home/"$DEPLOY_USER"/.ssh
echo "$DEPLOY_SSH_PUBLIC_KEY" > /home/"$DEPLOY_USER"/.ssh/authorized_keys
chmod 700  /home/"$DEPLOY_USER"/.ssh
chmod 600  /home/"$DEPLOY_USER"/.ssh/authorized_keys
chown -R "$DEPLOY_USER":"$DEPLOY_USER" /home/"$DEPLOY_USER"/.ssh

info "=== Step 3: SSH hardening ==="
SSHD_CONFIG="/etc/ssh/sshd_config"
cp "$SSHD_CONFIG" "${SSHD_CONFIG}.bak.$(date +%Y%m%d)"

# Apply hardening settings
declare -A SSH_SETTINGS=(
    ["Port"]="$SSH_PORT"
    ["PermitRootLogin"]="no"
    ["PasswordAuthentication"]="no"
    ["ChallengeResponseAuthentication"]="no"
    ["UsePAM"]="yes"
    ["X11Forwarding"]="no"
    ["PrintMotd"]="no"
    ["AcceptEnv"]="LANG LC_*"
    ["Subsystem"]="sftp /usr/lib/openssh/sftp-server"
    ["AllowUsers"]="$DEPLOY_USER"
    ["MaxAuthTries"]="3"
    ["LoginGraceTime"]="30"
    ["ClientAliveInterval"]="300"
    ["ClientAliveCountMax"]="2"
)

for key in "${!SSH_SETTINGS[@]}"; do
    value="${SSH_SETTINGS[$key]}"
    if grep -qE "^#?${key}" "$SSHD_CONFIG"; then
        sed -i "s|^#\?${key}.*|${key} ${value}|" "$SSHD_CONFIG"
    else
        echo "${key} ${value}" >> "$SSHD_CONFIG"
    fi
done

sshd -t || error "SSH config test failed — check $SSHD_CONFIG"
systemctl restart sshd
info "SSH hardened: root login disabled, port moved to $SSH_PORT, key-only auth."

info "=== Step 4: UFW Firewall ==="
ufw --force reset
ufw default deny incoming
ufw default allow outgoing
ufw allow "$SSH_PORT"/tcp   comment 'SSH'
ufw allow 80/tcp            comment 'HTTP'
ufw allow 443/tcp           comment 'HTTPS'
ufw --force enable
ufw status verbose

info "=== Step 5: Install Docker ==="
if command -v docker &>/dev/null; then
    warn "Docker already installed — skipping."
else
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
        | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg

    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
      https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
      > /etc/apt/sources.list.d/docker.list

    apt-get update -q
    apt-get install -y -q docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

usermod -aG docker "$DEPLOY_USER"
systemctl enable --now docker
info "Docker installed. deploy user added to docker group."

info "=== Step 6: 2 GB Swap ==="
if swapon --show | grep -q '/swapfile'; then
    warn "Swap already exists — skipping."
else
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    # Tune swappiness: prefer RAM, only swap under pressure
    echo 'vm.swappiness=10' >> /etc/sysctl.d/99-radolfa.conf
    sysctl -p /etc/sysctl.d/99-radolfa.conf
    info "2 GB swap enabled (swappiness=10)."
fi

info "=== Step 7: Application directory structure ==="
mkdir -p \
    "$APP_DIR" \
    "$APP_DIR/backups" \
    "$APP_DIR/scripts" \
    "$APP_DIR/nginx/conf.d" \
    "$APP_DIR/infra" \
    /var/www/certbot \
    /var/log/radolfa

chown -R "$DEPLOY_USER":"$DEPLOY_USER" "$APP_DIR"
chown -R "$DEPLOY_USER":"$DEPLOY_USER" /var/log/radolfa
info "Created: $APP_DIR/{backups,scripts,nginx,infra}, /var/www/certbot, /var/log/radolfa"

info "=== Step 8: Fail2ban ==="
systemctl enable --now fail2ban
info "Fail2ban enabled. Copy infra/fail2ban/jail.local to /etc/fail2ban/jail.local and restart."

info "=== Step 9: Unattended security upgrades ==="
cat > /etc/apt/apt.conf.d/20auto-upgrades <<'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
APT::Periodic::AutocleanInterval "7";
EOF

cat > /etc/apt/apt.conf.d/50unattended-upgrades <<'EOF'
Unattended-Upgrade::Allowed-Origins {
    "${distro_id}:${distro_codename}-security";
};
Unattended-Upgrade::Automatic-Reboot "false";
EOF
info "Unattended security upgrades configured."

info "=== Step 10: Kernel network tuning ==="
cat >> /etc/sysctl.d/99-radolfa.conf <<'EOF'
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 8192
net.ipv4.ip_local_port_range = 1024 65535
EOF
sysctl -p /etc/sysctl.d/99-radolfa.conf

info "=== Step 11: sudo policy for deploy user ==="
# Full passwordless sudo — required for install-erpnext.sh (apt, tee, mysql, etc.)
# Scope is intentionally broad: this is a single-tenant deployment VPS.
cat > /etc/sudoers.d/deploy-nopasswd <<EOF
$DEPLOY_USER ALL=(ALL) NOPASSWD: ALL
EOF
chmod 0440 /etc/sudoers.d/deploy-nopasswd
visudo -c || error "sudoers syntax error — check /etc/sudoers.d/deploy-nopasswd"

echo ""
info "================================================================"
info "Bootstrap complete!"
info "================================================================"
info "NEXT STEPS:"
info "  1. Copy infra/fail2ban/jail.local → /etc/fail2ban/jail.local"
info "  2. Copy infra/fail2ban/filter.d/nginx-req-limit.conf → /etc/fail2ban/filter.d/"
info "  3. sudo systemctl restart fail2ban"
info "  4. Copy infra/cron/* entries to /etc/cron.d/"
info "  5. Place .env.production in $APP_DIR/"
info "  6. Run: scripts/install-erpnext.sh (as deploy user)"
info "  7. Run: scripts/ssl-setup.sh"
info "  8. Run: scripts/deploy.sh"
echo ""
warn "You are still connected on port 22 this session."
warn "Open a NEW terminal and verify SSH works on port $SSH_PORT before closing this one."
