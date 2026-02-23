# Radolfa — Production Deployment Runbook

## Overview

| Component | Method | Host |
|---|---|---|
| Frontend (Next.js) | Docker container | radolfa.site |
| Backend (Spring Boot) | Docker container | api.radolfa.site |
| Elasticsearch | Docker container | internal |
| PostgreSQL | Docker container | internal |
| ERPNext v15 | bare-metal bench + Supervisor | erp.radolfa.site |
| Nginx | Docker container | reverse proxy for all 3 |
| SSL | Let's Encrypt (Certbot) | /etc/letsencrypt |

**VPS**: Ubuntu 22.04, 2 vCPU / 4 GB RAM (test deployment)
**ERPNext site name**: `erp.radolfa.site`

---

## Prerequisites

Before you start, ensure:

- [ ] VPS is running Ubuntu 22.04
- [ ] DNS A records point to the VPS IP:
  - `radolfa.site`     → VPS IP
  - `www.radolfa.site` → VPS IP
  - `api.radolfa.site` → VPS IP
  - `erp.radolfa.site` → VPS IP
- [ ] You have `root` SSH access for the initial bootstrap
- [ ] Repository cloned or files ready to SCP

---

## Step 1 — VPS Bootstrap (run once as root)

Edit `scripts/bootstrap-vps.sh` and fill in:
- `DEPLOY_SSH_PUBLIC_KEY` — your SSH public key

```bash
scp scripts/bootstrap-vps.sh root@<VPS_IP>:
ssh root@<VPS_IP>
bash bootstrap-vps.sh
```

This script:
- Creates the `deploy` user
- Hardens SSH (moves to port **2222**, disables password auth, disables root login)
- Sets UFW rules (allow: 80, 443, 2222)
- Installs Docker + Docker Compose plugin
- Adds `deploy` to the `docker` group
- Creates 2 GB swap
- Creates `/opt/radolfa/` directory structure
- Installs Fail2ban, Certbot, unattended-upgrades

> **WARNING**: After bootstrap, SSH is on port **2222**. Verify with a new terminal:
> ```bash
> ssh -p 2222 deploy@<VPS_IP>
> ```
> Before closing the root session.

---

## Step 2 — Deploy Fail2ban & Cron

```bash
# From the deploy user session on VPS
sudo cp /opt/radolfa/infra/fail2ban/jail.local /etc/fail2ban/jail.local
sudo cp /opt/radolfa/infra/fail2ban/filter.d/nginx-req-limit.conf \
        /etc/fail2ban/filter.d/nginx-req-limit.conf
sudo systemctl restart fail2ban

sudo cp /opt/radolfa/infra/cron/backup        /etc/cron.d/radolfa-backup
sudo cp /opt/radolfa/infra/cron/certbot-renew /etc/cron.d/certbot-renew
sudo chmod 644 /etc/cron.d/radolfa-backup /etc/cron.d/certbot-renew
```

---

## Step 3 — ERPNext v15 Installation

Edit `scripts/install-erpnext.sh` and fill in:
- `MARIADB_ROOT_PASSWORD`
- `SITE_ADMIN_PASSWORD`

```bash
# As deploy user on the VPS
bash /opt/radolfa/scripts/install-erpnext.sh
```

This script:
- Installs Node.js 18, MariaDB 10.6, Redis, wkhtmltopdf
- Configures MariaDB charset for Frappe (utf8mb4)
- Installs frappe-bench via pip
- Creates bench at `/opt/frappe/frappe-bench`
- Creates site `erp.radolfa.site`
- Installs ERPNext v15
- Sets up Supervisor (NOT nginx — we use our own Docker nginx)
- Configures 2 gunicorn workers (conserves RAM on test VPS)

After installation:
- Gunicorn: `localhost:8000`
- Socket.io: `localhost:9000`

> **IMPORTANT**: Do NOT run `bench setup nginx`. Our Docker nginx handles all routing.

---

## Step 4 — Production Environment File

Create `/opt/radolfa/.env.production` on the VPS (copy from `.env.production` in repo and fill in all secrets):

```bash
scp .env.production deploy@<VPS_IP>:/opt/radolfa/.env.production
ssh -p 2222 deploy@<VPS_IP> "chmod 600 /opt/radolfa/.env.production"
```

**Required values to fill in:**

| Variable | How to generate |
|---|---|
| `POSTGRES_PASSWORD` | `openssl rand -base64 32` |
| `JWT_SECRET` | `openssl rand -base64 64` |
| `SYSTEM_API_KEY` | `openssl rand -hex 32` |
| `AWS_ACCESS_KEY_ID` | From Timeweb Cloud console |
| `AWS_SECRET_ACCESS_KEY` | From Timeweb Cloud console |
| `AWS_S3_BUCKET` | Your bucket name |

---

## Step 5 — Copy Deployment Files to VPS

```bash
scp -P 2222 docker-compose.prod.yml deploy@<VPS_IP>:/opt/radolfa/
scp -P 2222 -r nginx/              deploy@<VPS_IP>:/opt/radolfa/nginx/
scp -P 2222 -r scripts/            deploy@<VPS_IP>:/opt/radolfa/scripts/
chmod +x /opt/radolfa/scripts/*.sh   # on VPS
```

---

## Step 6 — SSL Certificates

Edit `scripts/ssl-setup.sh` and set `CERTBOT_EMAIL`.

All 4 DNS records must resolve before running:

```bash
# As root on VPS
sudo bash /opt/radolfa/scripts/ssl-setup.sh
```

This obtains certs for `radolfa.site`, `www.radolfa.site`, `api.radolfa.site`, `erp.radolfa.site`.

---

## Step 7 — First Deployment

### Option A — Manual (before CI/CD is wired up)

```bash
# Log in to GHCR first (or pull images manually)
docker login ghcr.io -u idibek-alimov

# On VPS as deploy user
cd /opt/radolfa
bash scripts/deploy.sh latest
```

### Option B — Via GitHub Actions (after Step 8)

Push to `main` and the pipeline deploys automatically.

---

## Step 8 — GitHub Actions Setup

Add the following **repository secrets** at:
`https://github.com/idibek-alimov/claude-radolfa/settings/secrets/actions`

| Secret | Value |
|---|---|
| `VPS_HOST` | VPS IP address |
| `VPS_SSH_PORT` | `2222` |
| `VPS_DEPLOY_USER` | `deploy` |
| `VPS_SSH_PRIVATE_KEY` | Private key of the deploy user's SSH keypair |
| `GHCR_TOKEN` | GitHub PAT with `packages:write` scope |
| `NEXT_PUBLIC_API_BASE_URL` | `https://api.radolfa.site` |

> For `VPS_SSH_PRIVATE_KEY`: generate a dedicated keypair for GitHub Actions:
> ```bash
> ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/radolfa_deploy
> # Add .pub to /home/deploy/.ssh/authorized_keys on VPS
> # Paste private key into GitHub secret
> ```

**Optional**: Configure the `production` GitHub Environment (Settings → Environments → production) with required reviewers for manual approval before each deploy.

---

## Ongoing Operations

### Check container status

```bash
docker compose -f /opt/radolfa/docker-compose.prod.yml ps
```

### View logs

```bash
# All services
docker compose -f /opt/radolfa/docker-compose.prod.yml logs -f

# Single service
docker compose -f /opt/radolfa/docker-compose.prod.yml logs -f backend
docker compose -f /opt/radolfa/docker-compose.prod.yml logs -f nginx
```

### ERPNext logs

```bash
cd /opt/frappe/frappe-bench
bench logs --service gunicorn
bench logs --service worker
```

### ERPNext status

```bash
cd /opt/frappe/frappe-bench
bench status
```

### Manual backup

```bash
bash /opt/radolfa/scripts/backup.sh
ls /opt/radolfa/backups/
```

### Memory check (important on 4 GB test VPS)

```bash
free -h
docker stats --no-stream
```

---

## Rollback Procedure

### Via GitHub Actions

If the deploy job fails, the rollback step automatically restores `:latest`.

### Manual rollback to a specific tag

```bash
# On VPS as deploy user
bash /opt/radolfa/scripts/deploy.sh <previous-sha>
# Example:
bash /opt/radolfa/scripts/deploy.sh a1b2c3d4
```

### Manual rollback to :latest

```bash
bash /opt/radolfa/scripts/deploy.sh latest
```

---

## Database Restore

```bash
# List available backups
ls -lh /opt/radolfa/backups/

# Restore (OVERWRITES current DB — will prompt for confirmation)
bash /opt/radolfa/scripts/restore.sh /opt/radolfa/backups/postgres_20260101_020000.sql.gz
```

---

## Upgrading from Test to Production VPS

When you upgrade to a larger VPS (≥8 GB RAM):

1. Increase `ES_HEAP_SIZE` in `.env.production` (e.g. `512m` or `1g`)
2. Increase `JAVA_OPTS` backend heap (e.g. `-Xms512m -Xmx1g`)
3. Update ERPNext gunicorn workers: `bench config gunicorn_workers 4`
4. Update memory limits in `docker-compose.prod.yml`
5. Redeploy: `bash scripts/deploy.sh latest`

---

## Security Checklist

- [ ] All secrets in `.env.production` rotated from dev values
- [ ] `/opt/radolfa/.env.production` has permissions `600`
- [ ] `SYSTEM_API_KEY` matches the value configured in ERPNext
- [ ] Fail2ban is running: `sudo systemctl status fail2ban`
- [ ] UFW is active: `sudo ufw status`
- [ ] SSH root login disabled: `sshd -T | grep permitrootlogin`
- [ ] Certbot auto-renewal cron installed: `cat /etc/cron.d/certbot-renew`
- [ ] No containers expose internal ports externally (db, elasticsearch, backend, frontend)
