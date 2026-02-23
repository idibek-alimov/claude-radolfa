# Radolfa — Production Deployment Guide

**Domain:** radolfa.site · api.radolfa.site · erp.radolfa.site
**VPS:** Ubuntu 22.04, 2 vCPU / 4 GB RAM
**ERPNext site name:** `erp.radolfa.site`

---

## Quick Reference

| Thing | Value |
|---|---|
| SSH port | **2222** (moved from 22 during bootstrap) |
| Deploy user | `deploy` |
| App directory | `/opt/radolfa` |
| ERPNext bench | `/opt/frappe/frappe-bench` |
| ERPNext gunicorn | `localhost:8000` |
| ERPNext socket.io | `localhost:9000` |
| Backups | `/opt/radolfa/backups` |
| Logs | `/var/log/radolfa` |
| SSL certs | `/etc/letsencrypt/live/` |

---

## Prerequisites Checklist

Before you touch the VPS:

- [x] VPS provisioned — `185.207.65.241` (Ubuntu 22.04, 2 vCPU, 4 GB RAM)
- [x] Timeweb Cloud S3 bucket ready — credentials already in `.env.production`
- [x] GitHub repo — `idibek-alimov/claude-radolfa`
- [ ] You have a **local SSH keypair** (`ssh-keygen -t ed25519` if not)
- [ ] DNS A records set at your registrar (see Phase 0)
- [ ] Three secrets generated and placed in `.env.production` (see "Generating Secrets" below)

### Generating Secrets

You need to generate **three** values. Run these on your local machine and paste the output into `.env.production`:

```bash
# 1. PostgreSQL password  →  POSTGRES_PASSWORD
openssl rand -base64 32

# 2. JWT secret           →  JWT_SECRET
openssl rand -base64 64

# 3. ERPNext sync key     →  SYSTEM_API_KEY  (also set this same value in ERPNext — Phase 2.4)
openssl rand -hex 32
```

S3 credentials, VPS IP, and all domains are already configured.

---

## Phase 0 — DNS Setup

At your domain registrar, create these A records pointing to your VPS IP:

| Record | Type | Value |
|---|---|---|
| `radolfa.site` | A | `185.207.65.241` |
| `www.radolfa.site` | A | `185.207.65.241` |
| `api.radolfa.site` | A | `185.207.65.241` |
| `erp.radolfa.site` | A | `185.207.65.241` |

TTL: 300 seconds (5 min) while deploying, raise to 3600 after.

**Verify DNS propagation before continuing:**
```bash
dig +short A radolfa.site
dig +short A api.radolfa.site
dig +short A erp.radolfa.site
```
All four must return your VPS IP before you run certbot.

---





## Phase 1 — VPS Bootstrap

### 1.1 — Prepare the bootstrap script

Open `scripts/bootstrap-vps.sh` and fill in the single required value:

```bash
# Line ~18 — paste the output of: cat ~/.ssh/id_ed25519.pub
DEPLOY_SSH_PUBLIC_KEY="ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMqRQnLTc0bLWwvuyCyosVZjA+lA3e+p8GY33sbPivbs idibek@idibek-Legion-Pro-5-16IRX9"
```

### 1.2 — Copy and run as root

```bash
# From your local machine (root password is in your Timeweb Cloud control panel)
scp scripts/bootstrap-vps.sh root@185.207.65.241:
ssh root@185.207.65.241
bash bootstrap-vps.sh
```

This takes 3–5 minutes. It will:
- Create the `deploy` user
- Move SSH to **port 2222** and disable root login + passwords
- Enable UFW (allows 80, 443, 2222 only)
- Install Docker + Docker Compose plugin
- Create 2 GB swap
- Create `/opt/radolfa/` structure
- Install Fail2ban, Certbot

### 1.3 — CRITICAL: Verify the new SSH port before closing root session

Open a **new terminal** and confirm:
```bash
ssh -p 2222 deploy@185.207.65.241
```

If that works — you're safe to close the root session.
If it fails — do NOT close root. Check `/etc/ssh/sshd_config`.

> From this point on, always connect as `deploy` on port **2222**.

---

## Phase 2 — ERPNext v15 Installation

### 2.1 — Prepare the install script

Open `scripts/install-erpnext.sh` and set:

```bash
MARIADB_ROOT_PASSWORD="a_strong_password_for_mariadb_root"
SITE_ADMIN_PASSWORD="a_strong_password_for_erpnext_administrator"
```

Write these down — you'll need them for ERPNext admin access.

### 2.2 — Copy and run as deploy user

```bash
# Copy to VPS
scp -P 2222 scripts/install-erpnext.sh deploy@185.207.65.241:/opt/radolfa/scripts/

# SSH in and run
ssh -p 2222 deploy@185.207.65.241
chmod +x /opt/radolfa/scripts/install-erpnext.sh
bash /opt/radolfa/scripts/install-erpnext.sh
```

This takes **30–50 minutes** (Node 20, MariaDB, fetching ERPNext v15 + POSAwesome from GitHub, building frontend assets).

### 2.3 — Verify ERPNext is running

```bash
# Check gunicorn is listening on port 8000
ss -tlnp | grep ':8000'

# Check supervisor status
sudo supervisorctl status

# View ERPNext logs
cd /opt/frappe/frappe-bench && bench logs
```

Expected output from `ss`: something like `LISTEN 0 ... 0.0.0.0:8000`

### 2.4 — Set the SYSTEM_API_KEY in ERPNext

This key authenticates backend-to-ERP calls. It must match `SYSTEM_API_KEY` in your `.env.production`.

1. Open `http://185.207.65.241:8000` in your browser (temporarily accessible on port 8000 before nginx is up)
2. Log in as Administrator
3. Go to **Settings → System Settings** or search for "System Settings"
4. Find the field `system_api_key` (or create a custom field via Customization)

> **Alternative — set via bench:**
> ```bash
> cd /opt/frappe/frappe-bench
> bench --site erp.radolfa.site set-config system_api_key "your_SYSTEM_API_KEY_value"
> ```

---

## Phase 3 — Prepare Environment File

### 3.1 — Fill in `.env.production`

On your **local machine**, open `.env.production`. S3 credentials are already filled in.
You only need to fill in the three secrets that must be generated:

```
POSTGRES_PASSWORD=<run: openssl rand -base64 32>
JWT_SECRET=<run: openssl rand -base64 64>
SYSTEM_API_KEY=<run: openssl rand -hex 32>  ← also set this in ERPNext (Phase 2.4)
```

S3 values are already set in the file (bucket, access key, secret key, region, endpoint).

Leave `IMAGE_TAG=latest` — CI/CD will update this automatically.

### 3.2 — Copy to VPS

```bash
scp -P 2222 .env.production deploy@185.207.65.241:/opt/radolfa/.env.production
ssh -p 2222 deploy@185.207.65.241 "chmod 600 /opt/radolfa/.env.production"
```

**Verify it's not world-readable:**
```bash
ssh -p 2222 deploy@185.207.65.241 "ls -la /opt/radolfa/.env.production"
# Should show: -rw------- (600)
```

---

## Phase 4 — Copy Deployment Files to VPS

Copy the docker-compose, nginx configs, and scripts to the VPS:

```bash
# From your local machine (repo root)
scp -P 2222 docker-compose.prod.yml          deploy@185.207.65.241:/opt/radolfa/
scp -P 2222 -r nginx/                        deploy@185.207.65.241:/opt/radolfa/
scp -P 2222 -r scripts/                      deploy@185.207.65.241:/opt/radolfa/
scp -P 2222 -r infra/                        deploy@185.207.65.241:/opt/radolfa/

# Make scripts executable on VPS
ssh -p 2222 deploy@185.207.65.241 "chmod +x /opt/radolfa/scripts/*.sh"
```

### 4.1 — Deploy Fail2ban and cron jobs

```bash
ssh -p 2222 deploy@185.207.65.241

# On the VPS:
sudo cp /opt/radolfa/infra/fail2ban/jail.local /etc/fail2ban/jail.local
sudo cp /opt/radolfa/infra/fail2ban/filter.d/nginx-req-limit.conf \
        /etc/fail2ban/filter.d/nginx-req-limit.conf
sudo systemctl restart fail2ban
sudo systemctl status fail2ban   # should show: active (running)

sudo cp /opt/radolfa/infra/cron/backup        /etc/cron.d/radolfa-backup
sudo cp /opt/radolfa/infra/cron/certbot-renew /etc/cron.d/certbot-renew
sudo chmod 644 /etc/cron.d/radolfa-backup /etc/cron.d/certbot-renew
```

---

## Phase 5 — SSL Certificates

### 5.1 — Confirm DNS resolves before running

```bash
# All four must return your VPS IP
dig +short A radolfa.site api.radolfa.site erp.radolfa.site www.radolfa.site
```

If any returns empty or wrong IP — **do not proceed**. Wait for DNS propagation.

### 5.2 — Run certbot

```bash
ssh -p 2222 deploy@185.207.65.241
sudo bash /opt/radolfa/scripts/ssl-setup.sh
```

The script will:
1. Stop any running nginx container (frees port 80)
2. Run certbot `--standalone` for all 3 domains
3. Start the application stack

If certbot fails with "could not bind to port 80": something is using port 80.
Check: `sudo ss -tlnp | grep ':80'`

### 5.3 — Verify certificates

```bash
sudo certbot certificates
```

Expected output: 3 certificates listed for radolfa.site, api.radolfa.site, erp.radolfa.site.

---

## Phase 6 — First Deployment (Manual)

This deploys via GitHub Container Registry. You need to push images first, OR do a manual build on the VPS.

### Option A — Push images from CI/CD (recommended)

Complete Phase 7 (GitHub Actions setup) first, then push to `main` to trigger the pipeline. Come back here after images are pushed.

### Option B — Build images directly on VPS (for first boot)

```bash
ssh -p 2222 deploy@185.207.65.241
cd /opt/radolfa

# This builds locally — takes 5-10 min on a small VPS
docker compose -f docker-compose.prod.yml \
               --env-file .env.production \
               build --no-cache

docker compose -f docker-compose.prod.yml \
               --env-file .env.production \
               up -d
```

> Note: local build is only for first boot. After CI/CD is set up, it pulls pre-built images from GHCR.

### 6.1 — Watch startup

```bash
docker compose -f docker-compose.prod.yml logs -f
```

Expected startup order:
1. `db` (healthy in ~10 s)
2. `elasticsearch` (healthy in ~60 s — slow on first boot)
3. `backend` (healthy in ~90 s — Flyway migrations run here)
4. `frontend` (healthy in ~30 s after backend)
5. `nginx` (up in ~5 s)

Flyway will run all 13 migrations on first boot. V5 seeds the 3 users (USER / MANAGER / SYSTEM roles).

### 6.2 — Verify all containers are healthy

```bash
docker compose -f docker-compose.prod.yml ps
```

All containers should show `healthy`. If any show `unhealthy`:
```bash
docker compose -f docker-compose.prod.yml logs <service-name> --tail=50
```

### 6.3 — Smoke tests

```bash
# Nginx responding
curl -I https://radolfa.site

# Backend API health
curl -s https://api.radolfa.site/actuator/health | python3 -m json.tool

# ERPNext (should show ERPNext login page)
curl -I https://erp.radolfa.site
```

---

## Phase 7 — GitHub Actions CI/CD

### 7.1 — Create a GHCR personal access token

1. Go to: https://github.com/settings/tokens/new
   (or: GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic))
2. Name: `radolfa-deploy`
3. Expiry: No expiration (or 1 year — set a calendar reminder to rotate)
4. Scopes: ✅ `write:packages`, ✅ `read:packages`, ✅ `delete:packages`
5. Generate and copy the token

### 7.2 — Create a deploy SSH keypair

This is a **separate** keypair — NOT your personal SSH key. It's stored in GitHub Secrets.

```bash
# On your local machine
ssh-keygen -t ed25519 -C "github-actions-radolfa" -f ~/.ssh/radolfa_github_deploy
# Press Enter twice (no passphrase)

# Copy public key to the VPS
ssh-copy-id -p 2222 -i ~/.ssh/radolfa_github_deploy.pub deploy@185.207.65.241

# Print the private key — you'll paste this into GitHub Secrets
cat ~/.ssh/radolfa_github_deploy
```

### 7.3 — Add GitHub Actions Secrets

Go to: **https://github.com/idibek-alimov/claude-radolfa/settings/secrets/actions**

Click **"New repository secret"** and add each of the following:

| Secret Name | Value |
|---|---|
| `VPS_HOST` | `185.207.65.241` |
| `VPS_SSH_PORT` | `2222` |
| `VPS_DEPLOY_USER` | `deploy` |
| `VPS_SSH_PRIVATE_KEY` | Contents of `~/.ssh/radolfa_github_deploy` (the private key, including the `-----BEGIN...` lines) |
| `GHCR_TOKEN` | The GitHub PAT you created in 7.1 |
| `NEXT_PUBLIC_API_BASE_URL` | `https://api.radolfa.site` |

### 7.4 — (Optional) Set up production environment with approval gate

Go to: **https://github.com/idibek-alimov/claude-radolfa/settings/environments**

1. Click **"New environment"** → name it `production`
2. Enable **"Required reviewers"** and add yourself
3. This means every push to `main` will wait for your manual approval before deploying to VPS

### 7.5 — Trigger first CI/CD pipeline

```bash
# Push any commit to main
git add -A
git commit -m "chore: configure production deployment"
git push origin main
```

Watch the pipeline at: **https://github.com/idibek-alimov/claude-radolfa/actions**

Expected flow:
1. `CI` job runs (backend tests + frontend build) — ~5 min
2. `Deploy to Production` → `build-and-push` job — builds Docker images, pushes to GHCR — ~10 min
3. `deploy` job — SSHes into VPS, pulls images, restarts containers — ~3 min

---

## Phase 8 — ERPNext Post-Configuration

After everything is running, configure ERPNext to work with Radolfa.

### 8.1 — Set site URL

```bash
ssh -p 2222 deploy@185.207.65.241
cd /opt/frappe/frappe-bench
bench --site erp.radolfa.site set-config host_name "https://erp.radolfa.site"
```

### 8.2 — Sync initial data to Radolfa

Once you have products, categories, and users in ERPNext, trigger the sync via the backend:

```bash
# Sync product hierarchy (categories)
curl -X POST https://api.radolfa.site/api/erp-sync/product-hierarchy \
  -H "X-System-Api-Key: <your_SYSTEM_API_KEY>"

# Sync products
curl -X POST https://api.radolfa.site/api/erp-sync/products \
  -H "X-System-Api-Key: <your_SYSTEM_API_KEY>"

# Sync users/customers
curl -X POST https://api.radolfa.site/api/erp-sync/users \
  -H "X-System-Api-Key: <your_SYSTEM_API_KEY>"
```

---

## Ongoing Operations

### Deploy a new version

Happens automatically on push to `main`. For manual deploy:
```bash
ssh -p 2222 deploy@185.207.65.241
bash /opt/radolfa/scripts/deploy.sh
```

### Rollback to a specific version

```bash
# List available image tags in GHCR (or use a git SHA)
bash /opt/radolfa/scripts/deploy.sh a1b2c3d4   # 8-char git SHA
bash /opt/radolfa/scripts/deploy.sh latest     # latest stable
```

### View logs

```bash
# All containers
docker compose -f /opt/radolfa/docker-compose.prod.yml logs -f

# Single service
docker compose -f /opt/radolfa/docker-compose.prod.yml logs -f backend
docker compose -f /opt/radolfa/docker-compose.prod.yml logs -f nginx

# ERPNext
cd /opt/frappe/frappe-bench && bench logs
```

### Check resource usage (important on 4 GB VPS)

```bash
# Memory per container
docker stats --no-stream

# Overall memory
free -h

# Swap usage
swapon --show
```

### Manual backup

```bash
bash /opt/radolfa/scripts/backup.sh
ls -lh /opt/radolfa/backups/
```

Backups run automatically every day at 02:30 AM via cron. Last 7 days are kept.

### Restore from backup

```bash
# List backups
ls /opt/radolfa/backups/

# Restore (will prompt for confirmation — overwrites current DB)
bash /opt/radolfa/scripts/restore.sh /opt/radolfa/backups/postgres_20260201_023000.sql.gz
```

### Restart a stuck container

```bash
docker compose -f /opt/radolfa/docker-compose.prod.yml restart backend
docker compose -f /opt/radolfa/docker-compose.prod.yml restart nginx
```

### Restart ERPNext

```bash
cd /opt/frappe/frappe-bench
bench restart
# or
sudo supervisorctl restart all
```

---

## Troubleshooting

### "502 Bad Gateway" on radolfa.site

The frontend container is down or starting up.
```bash
docker compose -f /opt/radolfa/docker-compose.prod.yml ps frontend
docker compose -f /opt/radolfa/docker-compose.prod.yml logs frontend --tail=30
```

### "502 Bad Gateway" on api.radolfa.site

The backend container is down. Check if it's still running Flyway migrations on startup (normal for ~90 s on first boot).
```bash
docker compose -f /opt/radolfa/docker-compose.prod.yml logs backend --tail=50
```

### "502 Bad Gateway" on erp.radolfa.site

ERPNext gunicorn is not running on host port 8000.
```bash
ss -tlnp | grep ':8000'
cd /opt/frappe/frappe-bench && bench status
sudo supervisorctl status
```

If supervisor shows gunicorn stopped:
```bash
sudo supervisorctl start frappe-bench-web:frappe-bench-frappe-web-1
```

### SSL certificate error / HTTPS not working

Check certs exist:
```bash
sudo certbot certificates
ls /etc/letsencrypt/live/
```

If missing, re-run: `sudo bash /opt/radolfa/scripts/ssl-setup.sh`

### Backend Flyway checksum error on startup

If you ran the old V6/V11 migrations on a dev database and are now deploying to production with the new empty versions, Flyway will reject the checksum mismatch.

**For a fresh production database** — this won't happen. The problem only occurs if you're migrating an existing dev database.

If you hit this: run `./mvnw flyway:repair -Dflyway.url=...` or wipe and recreate the database.

### Container won't start — "out of memory"

The 4 GB VPS is tight. Check what's using memory:
```bash
docker stats --no-stream
free -h
```

Most likely culprit is Elasticsearch. Reduce its heap:
```bash
# Edit .env.production and lower ES_HEAP_SIZE
nano /opt/radolfa/.env.production
# Change: ES_HEAP_SIZE=256m → ES_HEAP_SIZE=192m

# Restart Elasticsearch
docker compose -f /opt/radolfa/docker-compose.prod.yml restart elasticsearch
```

### GitHub Actions deploy fails — "Permission denied (publickey)"

The deploy SSH key on the VPS doesn't match the secret in GitHub.

```bash
# Check authorized_keys on VPS
cat /home/deploy/.ssh/authorized_keys

# The GitHub secret VPS_SSH_PRIVATE_KEY must be the matching private key
# Regenerate if needed (see Phase 7.2)
```

---

## Security Checklist (run after first deploy)

```bash
# 1. Confirm root SSH is disabled
sshd -T | grep permitrootlogin          # should say: permitrootlogin no

# 2. Confirm password auth is disabled
sshd -T | grep passwordauthentication   # should say: passwordauthentication no

# 3. Confirm firewall is active
sudo ufw status                          # should show: Status: active, rules for 80/443/2222

# 4. Confirm fail2ban is running
sudo fail2ban-client status              # should list active jails

# 5. Confirm no internal ports are exposed
docker compose -f /opt/radolfa/docker-compose.prod.yml ps
# Only nginx should show 0.0.0.0:80->80 and 0.0.0.0:443->443
# db / elasticsearch / backend / frontend should have NO port column

# 6. Confirm .env.production is private
ls -la /opt/radolfa/.env.production     # must show -rw------- (600)

# 7. Check SSL cert expiry
sudo certbot certificates | grep Expiry
```

---

## What to Change When You Upgrade the VPS

When you move to a larger VPS (≥ 8 GB recommended for production):

1. In `/opt/radolfa/.env.production`:
   ```
   ES_HEAP_SIZE=512m          # was 256m
   JAVA_OPTS=-Xms512m -Xmx1g # was -Xms256m -Xmx512m
   ```

2. In `docker-compose.prod.yml`, raise the memory limits:
   ```yaml
   elasticsearch: limits: memory: 1G
   backend:       limits: memory: 1G
   ```

3. Increase ERPNext workers:
   ```bash
   cd /opt/frappe/frappe-bench
   bench config gunicorn_workers 4
   sudo supervisorctl restart all
   ```

4. Redeploy: `bash /opt/radolfa/scripts/deploy.sh`
