# Radolfa — ERPNext Data Sync Guide

> Generated: 2026-03-20
> Context: Phase 10 removed all automatic ERP sync. Radolfa is now the source of truth.

---

## TL;DR

**Can you still sync data from ERPNext? Yes, but only manually via the Radolfa REST API.**

There is no automatic sync infrastructure left in the codebase. All sync use cases, scheduled jobs, and the `ErpProductClient` were deleted in Phase 10. If you need to import data from ERPNext, you must call Radolfa's admin API endpoints yourself — either via a script, a one-time migration tool, or a separately hosted sync service.

---

## 1. What Was Removed in Phase 10

The following infrastructure was fully deleted:

| Removed Component | What It Did |
|---|---|
| `ErpProductClient` | HTTP client that fetched product hierarchy (items, item groups, prices) from ERPNext's REST API |
| `ErpSyncJobConfig` | Spring `@Scheduled` jobs that ran sync on a cron schedule |
| `SyncProductHierarchyUseCase` | Pulled products, variants (colors), SKUs (sizes) from ERPNext → wrote to Radolfa DB |
| `SyncCategoriesUseCase` | Pulled Item Groups from ERPNext → wrote to Radolfa category tree |
| `SyncDiscountUseCase` | Pulled Pricing Rules from ERPNext → wrote to Radolfa discount table |
| `SyncLoyaltyPointsUseCase` | Pulled loyalty point balances per customer from ERPNext |
| `SyncLoyaltyTiersUseCase` | Pulled loyalty program tier definitions from ERPNext |
| `SyncOrdersUseCase` | Pushed Radolfa orders into ERPNext Sales Orders |
| `SyncUsersUseCase` | Pulled ERPNext customer records → created Radolfa user accounts |
| `ROLE_SYSTEM` | Special JWT role used for machine-to-machine sync authentication |

---

## 2. Current State (Post Phase 10)

Radolfa manages all data natively. The data ownership is:

| Data | Owner | How to Manage |
|---|---|---|
| Products (base, variants, SKUs) | Radolfa | Admin API: `POST /api/v1/admin/products` |
| Prices | Radolfa | Admin API: `PUT /api/v1/admin/skus/{id}/price` |
| Stock | Radolfa | Admin API: `PUT /api/v1/admin/skus/{id}/stock` |
| Categories | Radolfa | Admin API: `POST /api/v1/admin/categories` |
| Discounts | Radolfa | Directly in DB (no admin API yet) |
| Loyalty tiers | Radolfa | Admin API: `POST /api/v1/admin/loyalty-tiers` |
| Loyalty points | Radolfa | Awarded automatically on payment confirmation |
| Users | Radolfa | Auto-created on first OTP login |
| Orders | Radolfa | Created via checkout flow |

ERPNext is **no longer read from or written to** by the Radolfa backend.

---

## 3. If You Want to Import ERPNext Data Into Radolfa

You can write a migration script (in any language) that:

1. Reads data from ERPNext via its Frappe REST API.
2. Writes it into Radolfa via the Radolfa admin API using an ADMIN JWT token.

### Step 1 — Get an ADMIN JWT token for Radolfa

```bash
# Send OTP to admin phone
curl -X POST https://your-radolfa.com/api/v1/auth/otp/send \
  -H 'Content-Type: application/json' \
  -d '{"phone": "+992XXXXXXXXX"}'

# Verify OTP and receive tokens
curl -X POST https://your-radolfa.com/api/v1/auth/otp/verify \
  -H 'Content-Type: application/json' \
  -d '{"phone": "+992XXXXXXXXX", "otp": "1234"}'

# Save the accessToken — use it as: Authorization: Bearer <token>
```

Make sure the user has `ADMIN` role in the `radolfa_user` table.

---

### Step 2 — Read from ERPNext

ERPNext exposes a REST API at `https://erp.radolfa.site/api/resource/`.

**Categories (Item Groups)**
```bash
GET https://erp.radolfa.site/api/resource/Item Group?fields=["name","parent_item_group"]&limit_page_length=100
```

**Products (Items)**
```bash
GET https://erp.radolfa.site/api/resource/Item?fields=["item_code","item_name","item_group","has_variants","variant_of"]&limit_page_length=200
```

**Prices (Item Price)**
```bash
GET https://erp.radolfa.site/api/resource/Item Price?fields=["item_code","price_list_rate","currency"]&filters=[["price_list","=","Standard Selling"]]
```

**Pricing Rules (Discounts)**
```bash
GET https://erp.radolfa.site/api/resource/Pricing Rule?fields=["name","items","rate","valid_from","valid_upto","disable"]
```

All ERPNext API calls require authentication:
```bash
# Option A: API key auth
-H 'Authorization: token <api_key>:<api_secret>'

# Option B: Session cookie (not recommended for scripts)
```

---

### Step 3 — Write to Radolfa API

**Create categories:**
```bash
curl -X POST https://your-radolfa.com/api/v1/admin/categories \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{"name": "Hoodies", "parentId": 1}'
```

**Create a product with variants and SKUs:**
```bash
curl -X POST https://your-radolfa.com/api/v1/admin/products \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Classic Hoodie",
    "categoryId": 2,
    "variants": [
      {
        "colorId": 1,
        "skus": [
          {"sizeLabel": "M", "price": 150.00, "stockQuantity": 30}
        ]
      }
    ]
  }'
```

**Update price:**
```bash
curl -X PUT https://your-radolfa.com/api/v1/admin/skus/101/price \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{"price": 165.00}'
```

**Update stock:**
```bash
curl -X PUT https://your-radolfa.com/api/v1/admin/skus/101/stock \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{"quantity": 50}'
```

---

## 4. What a Full Migration Script Would Look Like

This is pseudo-code for a one-time ERPNext → Radolfa data migration:

```python
import requests

ERP_BASE = "https://erp.radolfa.site/api/resource"
ERP_AUTH = ("api_key", "api_secret")  # or token header

RADOLFA_BASE = "https://your-radolfa.com/api/v1"
RADOLFA_TOKEN = "<admin_jwt_token>"
HEADERS = {"Authorization": f"Bearer {RADOLFA_TOKEN}", "Content-Type": "application/json"}

# 1. Sync categories (Item Groups → Categories)
erp_groups = requests.get(f"{ERP_BASE}/Item Group", auth=ERP_AUTH).json()
category_map = {}  # erp_name → radolfa_id
for group in erp_groups["data"]:
    parent_id = category_map.get(group["parent_item_group"])
    res = requests.post(f"{RADOLFA_BASE}/admin/categories",
                        json={"name": group["name"], "parentId": parent_id},
                        headers=HEADERS)
    category_map[group["name"]] = res.json()["id"]

# 2. Sync products and SKUs (Items → Products)
erp_items = requests.get(f"{ERP_BASE}/Item", auth=ERP_AUTH).json()
for item in erp_items["data"]:
    if item["variant_of"]:
        continue  # Skip variants — handled as SKUs
    category_id = category_map.get(item["item_group"])
    # You'll need to map colors — create or find by colorKey
    res = requests.post(f"{RADOLFA_BASE}/admin/products",
                        json={
                            "name": item["item_name"],
                            "categoryId": category_id,
                            "variants": [
                                {
                                    "colorId": 1,  # map from ERP variant attributes
                                    "skus": [
                                        {"sizeLabel": "S", "price": 0.0, "stockQuantity": 0}
                                    ]
                                }
                            ]
                        },
                        headers=HEADERS)

# 3. Sync prices (Item Price → SKU prices)
erp_prices = requests.get(f"{ERP_BASE}/Item Price",
                           params={"filters": '[["price_list","=","Standard Selling"]]'},
                           auth=ERP_AUTH).json()
for price in erp_prices["data"]:
    sku_id = radolfa_sku_id_from_code(price["item_code"])  # your lookup map
    requests.put(f"{RADOLFA_BASE}/admin/skus/{sku_id}/price",
                 json={"price": price["price_list_rate"]},
                 headers=HEADERS)
```

---

## 5. Ongoing ERPNext → Radolfa Sync (If Needed)

If you want **ongoing** automatic sync (e.g., nightly price/stock updates from ERP), your options are:

### Option A: External Sync Service (Recommended)

Build and deploy a separate microservice (Python, Node.js, or even Java) that:
1. Runs on a schedule (cron or Celery beat).
2. Reads from ERPNext REST API.
3. Calls Radolfa admin API with an ADMIN JWT token.
4. Runs independently — not part of the Radolfa Spring Boot app.

**Pros:** Radolfa stays clean. Sync logic is independently deployable and testable.

### Option B: Frappe Webhooks → Radolfa Webhook Endpoint

Add a webhook endpoint to Radolfa (a new controller in `infrastructure/web/`) that ERPNext calls when products/prices change.

Example new endpoint:
```
POST /api/v1/webhooks/erp/product-updated
X-Api-Key: <system-api-key>
Body: { "itemCode": "ITEM-001", "priceListRate": 165.00, "stockQuantity": 50 }
```

In ERPNext, configure Webhooks (Setup → Integrations → Webhooks) for:
- **Item** — on save → call your endpoint
- **Item Price** — on save → call your endpoint
- **Bin** (stock) — on update → call your endpoint

**Pros:** Near real-time sync. No polling.
**Cons:** Requires maintaining a new Radolfa endpoint. ERPNext webhook delivery is not guaranteed (no retry on failure).

### Option C: Re-add Scheduled Sync Inside Radolfa

If you want to bring back the scheduled sync that existed before Phase 10, you would need to:

1. Re-add `ErpProductClient` (Spring `@Bean` RestClient or WebClient pointing to ERPNext REST API).
2. Re-add sync use cases (read ERP data → transform → call Radolfa save ports).
3. Re-add `@Scheduled` jobs in a `ErpSyncJobConfig`.
4. Add `SYSTEM_API_KEY` authentication for the sync process (already exists in `ServiceApiKeyFilter`).

**The `ServiceApiKeyFilter` already exists** in the codebase — it validates `X-Api-Key` headers. You can reuse it for a sync launcher that authenticates as ADMIN.

**Not recommended** — Phase 10 removed this for a reason. Re-adding it couples Radolfa to ERPNext again and reintroduces the "who is the source of truth?" ambiguity.

---

## 6. What Data Radolfa Cannot Get From ERPNext Anymore

Even if you write a sync script, some data no longer flows from ERP automatically:

| Data | Why It's Blocked |
|---|---|
| **Sales Orders** | Radolfa creates orders natively. ERP is no longer the order system. |
| **Customer Loyalty Points** | Radolfa manages loyalty. ERP had a different program. |
| **User Accounts** | Radolfa creates users on first OTP login. ERP customers are not automatically Radolfa users. |

---

## 7. Summary

| Question | Answer |
|---|---|
| Is there automatic sync from ERPNext? | **No.** Removed in Phase 10. |
| Can I manually import from ERPNext? | **Yes.** Write a script that reads from ERPNext REST API and writes to Radolfa admin API. |
| Can I do ongoing sync? | **Yes, but externally.** Build a separate sync service or use ERPNext Webhooks → Radolfa webhook endpoint. |
| Should I re-add sync inside Radolfa? | **No.** It conflicts with the Phase 10 decision to make Radolfa the source of truth. |
| What API key does Radolfa use for system calls? | `ServiceApiKeyFilter` validates `X-Api-Key: <SYSTEM_API_KEY>` — configured via `SYSTEM_API_KEY` env var. |
| What Radolfa role is needed for data import? | `ADMIN` role. Use OTP login on an admin phone number. |
