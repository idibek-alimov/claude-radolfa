# Radolfa Test Data & Credentials

This document contains all test data created for development and testing purposes.

## Test Users

| Phone | Role | Permissions |
|-------|------|-------------|
| `+992901234567` | USER | View products, profile, wishlist, orders |
| `+992902345678` | MANAGER | Upload images, edit descriptions |
| `+992903456789` | SYSTEM | ERP sync operations (modify price/name/stock) |
| `+992904567890` | USER | Additional test user |
| `+992905678901` | MANAGER | Additional test manager |

## How to Login

1. Go to http://localhost:8000/login
2. Enter a phone number from the table above
3. Check backend logs for OTP:
   ```bash
   docker logs radolfa-backend 2>&1 | grep OTP | tail -1
   ```
4. Enter the 4-digit OTP
5. You'll receive a JWT token and be redirected

### Quick Login via API

```bash
# Step 1: Request OTP
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone": "+992902345678"}'

# Step 2: Get OTP from logs
docker logs radolfa-backend 2>&1 | grep "+992902345678" | tail -1

# Step 3: Verify OTP (replace 1234 with actual OTP)
curl -X POST http://localhost:8000/api/v1/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"phone": "+992902345678", "otp": "1234"}'

# Step 4: Use token in subsequent requests
curl http://localhost:8000/api/v1/users/me \
  -H "Authorization: Bearer <your-token>"
```

## Test Products

| ERP ID | Name | Price | Stock | Top Selling |
|--------|------|-------|-------|-------------|
| ERP-LAPTOP-001 | MacBook Pro 14" M3 | $1,999.99 | 25 | ✅ |
| ERP-PHONE-001 | iPhone 15 Pro Max | $1,199.99 | 50 | ✅ |
| ERP-AUDIO-001 | Sony WH-1000XM5 | $349.99 | 100 | ✅ |
| ERP-DISPLAY-001 | LG UltraFine 27" 5K | $1,299.99 | 15 | ❌ |
| ERP-INPUT-001 | Keychron Q1 Pro | $199.99 | 75 | ❌ |
| ERP-INPUT-002 | Logitech MX Master 3S | $99.99 | 120 | ✅ |
| ERP-TABLET-001 | iPad Pro 12.9" M2 | $1,099.99 | 30 | ❌ |
| ERP-WATCH-001 | Apple Watch Ultra 2 | $799.99 | 40 | ❌ |
| ERP-CAMERA-001 | Sony A7 IV | $2,499.99 | 0 | ❌ |
| ERP-AUDIO-002 | Sonos Era 300 | $449.99 | 60 | ❌ |

## API Endpoints

### Public Endpoints (No Auth Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | Request OTP |
| POST | `/api/v1/auth/verify` | Verify OTP, get JWT |
| GET | `/api/v1/products` | List all products |
| GET | `/api/v1/products/{erpId}` | Get product details |
| GET | `/actuator/health` | Health check |

### Protected Endpoints

| Method | Endpoint | Role Required | Description |
|--------|----------|---------------|-------------|
| POST | `/api/v1/products/{erpId}/images` | MANAGER | Upload product image |
| PUT | `/api/v1/products/{erpId}/description` | MANAGER | Update description |
| POST | `/api/v1/sync/products` | SYSTEM | Sync products from ERP |
| GET | `/api/v1/users/me` | Any authenticated | Get current user |

## Swagger UI

Access the full API documentation at: **http://localhost:8000/swagger-ui.html**

1. Click "Authorize" button
2. Enter: `Bearer <your-jwt-token>`
3. Test any endpoint directly from the browser

## Docker Commands

```bash
# Start all services
docker compose up -d

# View logs
docker logs radolfa-backend -f
docker logs radolfa-frontend -f

# Restart a service
docker compose restart backend

# Rebuild and restart
docker compose build backend && docker compose up -d backend

# Stop all services
docker compose down

# Stop and remove volumes (reset database)
docker compose down -v
```

## Database Access

```bash
# Connect to PostgreSQL
docker exec -it radolfa-db psql -U radolfa -d radolfa_db

# Useful queries
SELECT * FROM users;
SELECT id, erp_id, name, price, stock, is_top_selling FROM products;
SELECT * FROM erp_sync_log ORDER BY synced_at DESC LIMIT 10;
```

## Service URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:8000 |
| Login Page | http://localhost:8000/login |
| Products Page | http://localhost:8000/products |
| Swagger UI | http://localhost:8000/swagger-ui.html |
| Backend Direct | http://localhost:8080 |
| PostgreSQL | localhost:5432 |
| Elasticsearch | http://localhost:9200 |

## Environment Variables

See `.env.example` for all configuration options. Key variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_PASSWORD` | - | Database password |
| `JWT_SECRET` | - | JWT signing key (32+ chars) |
| `AWS_ACCESS_KEY_ID` | - | S3 access key |
| `AWS_SECRET_ACCESS_KEY` | - | S3 secret key |
| `AWS_S3_BUCKET` | - | S3 bucket name |

## File Locations

| File | Purpose |
|------|---------|
| `backend/src/main/resources/db/migration/V4__seed_data.sql` | Seed data migration |
| `.env` | Environment configuration |
| `.env.example` | Environment template |
| `docker-compose.yml` | Service orchestration |
| `nginx/nginx.conf` | Reverse proxy config |
