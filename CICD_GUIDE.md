# CI/CD Guide — Radolfa

## Is CI/CD configured?

**Yes, but it needs 6 GitHub Secrets set before it can run.**

Two workflows exist in `.github/workflows/`:

| Workflow | File | Triggers on |
|---|---|---|
| **CI** — runs tests & lint | `ci.yml` | Every push to any branch |
| **Deploy** — builds images, deploys to VPS | `deploy.yml` | Push to `main` only |

---

## How it works end-to-end

```
You push to main
       │
       ▼
┌─────────────────────────────────────────┐
│  CI (ci.yml)                            │
│  ├─ Backend: Maven tests + PostgreSQL   │
│  └─ Frontend: npm lint + next build     │
└─────────────────────────────────────────┘
       │ (runs in parallel with deploy)
       ▼
┌─────────────────────────────────────────┐
│  Deploy (deploy.yml) — Job 1            │
│  Build & push Docker images to GHCR     │
│  ├─ backend:a1b2c3d4  + backend:latest  │
│  └─ frontend:a1b2c3d4 + frontend:latest │
└─────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│  Deploy (deploy.yml) — Job 2            │
│  SSH into VPS (185.207.65.241:2222)     │
│  ├─ docker compose pull                 │
│  ├─ docker compose up -d               │
│  ├─ Wait 45s for health checks          │
│  └─ Smoke test: GET /nginx-health       │
└─────────────────────────────────────────┘
       │ if smoke test fails
       ▼
┌─────────────────────────────────────────┐
│  Auto-rollback                          │
│  Reverts IMAGE_TAG=latest and re-deploys│
└─────────────────────────────────────────┘
```

---

## Step 1 — Set the 6 required GitHub Secrets

Go to: **GitHub → your repo → Settings → Secrets and variables → Actions → New repository secret**

| Secret name | Value |
|---|---|
| `VPS_HOST` | `185.207.65.241` |
| `VPS_SSH_PORT` | `2222` |
| `VPS_DEPLOY_USER` | `deploy` |
| `VPS_SSH_PRIVATE_KEY` | contents of your `~/.ssh/id_ed25519` private key |
| `GHCR_TOKEN` | a GitHub PAT with `read:packages` + `write:packages` scope |
| `NEXT_PUBLIC_API_BASE_URL` | `https://api.radolfa.site` |

### How to get `VPS_SSH_PRIVATE_KEY`
```bash
cat ~/.ssh/id_ed25519
```
Copy the entire output including `-----BEGIN OPENSSH PRIVATE KEY-----` and `-----END OPENSSH PRIVATE KEY-----`.

### How to get `GHCR_TOKEN`
1. Go to **GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)**
2. Click **Generate new token (classic)**
3. Name it `GHCR deploy`
4. Check scopes: `write:packages`, `read:packages`, `delete:packages`
5. Copy the token — paste it as the `GHCR_TOKEN` secret

### About the `production` environment
The deploy workflow uses `environment: production`. This means you need to create it in GitHub:
1. **GitHub → repo → Settings → Environments → New environment**
2. Name it exactly: `production`
3. Optionally add yourself as a required reviewer (adds manual approval before each deploy)

---

## Step 2 — Test it with a simple real change

Here is a safe, visible change you can make to verify the full pipeline works.

### The change: update the app version in the backend

Open `backend/src/main/resources/application.yml` and change the info section, or simply add a comment to any file. The simplest option — add a line to the backend's main application file:

```bash
# On your local machine
cd /home/idibek/Desktop/ERP/claude-radolfa

# Make a trivial visible change
echo "" >> backend/src/main/resources/application.yml
echo "# CI/CD test deploy $(date)" >> backend/src/main/resources/application.yml

# Commit and push to main
git add backend/src/main/resources/application.yml
git commit -m "test: verify CI/CD pipeline end-to-end"
git push origin main
```

### What happens next (watch it live)

1. **Go to:** `https://github.com/idibek-alimov/claude-radolfa/actions`

2. You will see two workflows start simultaneously:
   - `CI` — runs Maven tests and Next.js build (~3–5 min)
   - `Deploy to Production` — builds Docker images and deploys (~8–12 min)

3. Click into **Deploy to Production** → you will see:

   ```
   ✅ Build & Push Images     (~5 min)  — builds backend + frontend, pushes to GHCR
   ✅ Deploy to VPS           (~3 min)  — SSHes into VPS, pulls images, restarts containers
   ```

4. When it finishes, verify the deploy happened:
   ```bash
   ssh -p 2222 deploy@185.207.65.241 "docker compose -f /opt/radolfa/docker-compose.prod.yml ps"
   ```
   The `CREATED` column will show a timestamp from just now.

5. Also check `https://api.radolfa.site/actuator/health` — it should return `{"status":"UP"}`.

---

## What each image tag means

Every deploy creates two tags:

| Tag | Meaning |
|---|---|
| `backend:a1b2c3d4` | The exact 8-char git SHA of the commit — permanent, never overwritten |
| `backend:latest` | Always points to the most recent successful deploy |

The VPS `.env.production` is updated with the SHA tag on each deploy:
```
IMAGE_TAG=a1b2c3d4
```
If a rollback is needed, the auto-rollback reverts to `:latest` (the previous good build).

---

## Manual deploy (without pushing code)

You can trigger a deploy from GitHub without making any code change:

1. Go to `https://github.com/idibek-alimov/claude-radolfa/actions/workflows/deploy.yml`
2. Click **Run workflow**
3. Leave `image_tag` blank to deploy the current latest, or enter a specific SHA like `a1b2c3d4` to deploy that exact version
4. Click **Run workflow**

---

## Troubleshooting

### Deploy fails at "Copy docker-compose.prod.yml to VPS"
→ `VPS_SSH_PRIVATE_KEY` or `VPS_HOST`/`VPS_SSH_PORT` secrets are wrong. Check them.

### Deploy fails at "Deploy" step with "permission denied"
→ The SSH key doesn't match the deploy user's `authorized_keys`. Re-run bootstrap or manually add the key:
```bash
ssh-copy-id -p 2222 -i ~/.ssh/id_ed25519.pub deploy@185.207.65.241
```

### Docker pull fails with "unauthorized"
→ `GHCR_TOKEN` is missing or expired. Generate a new one and update the secret.

### `production` environment not found
→ Create it in GitHub Settings → Environments as described in Step 1.

### Smoke test fails after deploy
→ The auto-rollback will trigger automatically. Check the deploy logs for the actual error, fix it, and push again.
