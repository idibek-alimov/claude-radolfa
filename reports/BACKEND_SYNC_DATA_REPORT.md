# Backend Sync Data Report

**Generated:** 2026-03-19  
**Source:** ERPNext → Radolfa E-commerce Backend  
**Base Path:** `/api/v1/sync`

---

## Overview

This report documents all data entities synchronized from ERPNext to the Radolfa e-commerce backend. ERPNext serves as the **SOURCE OF TRUTH** for price, name, and stock data.

All sync endpoints require:
- **Role:** `SYSTEM` (API key authentication)
- **Idempotency-Key header:** Required for most endpoints to prevent duplicate processing

---

## Sync Endpoints Summary

| Endpoint | Method | Data Type | Idempotency Required |
|----------|--------|-----------|---------------------|
| `/products` | POST | Product Hierarchy | ✅ Yes |
| `/categories` | POST | Categories | ❌ No |
| `/orders` | POST | Orders | ✅ Yes |
| `/users` | POST | Single User | ❌ No |
| `/users/batch` | POST | User Batch | ❌ No |
| `/loyalty` | POST | Loyalty Points | ✅ Yes |
| `/loyalty-tiers` | POST | Loyalty Tiers | ✅ Yes |
| `/discounts` | POST | Pricing Rule Discount | ✅ Yes |
| `/discounts` | DELETE | Remove Discount | ✅ Yes |

---

## 1. Product Hierarchy Sync

**Endpoint:** `POST /api/v1/sync/products`

### Description
Syncs the full product hierarchy from ERPNext following the structure:
**Template (ProductBase) → Variant (Colour) → Item (Size/SKU)**

### Request Format

```json
{
  "templateCode": "string (required, unique identifier)",
  "templateName": "string (required)",
  "category": "string (optional)",
  "variants": [
    {
      "colorKey": "string (required)",
      "items": [
        {
          "erpItemCode": "string (required, SKU code)",
          "sizeLabel": "string (optional)",
          "stockQuantity": "integer (required)",
          "listPrice": "decimal (required)"
        }
      ]
    }
  ]
}
```

### Response Format

```json
{
  "synced": "integer (count of successfully synced templates)",
  "errors": "integer (count of failed templates)",
  "skippedReason": "string (optional, present only if skipped)"
}
```

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `templateCode` | String | Unique ERPNext Item Template code |
| `templateName` | String | Display name of the product template |
| `category` | String | Category name (must be synced before products) |
| `colorKey` | String | Variant color identifier |
| `erpItemCode` | String | ERPNext SKU/item code |
| `sizeLabel` | String | Size designation (S, M, L, XL, etc.) |
| `stockQuantity` | Integer | Available stock from ERPNext |
| `listPrice` | Decimal | Price in currency units |

---

## 2. Categories Sync

**Endpoint:** `POST /api/v1/sync/categories`

### Description
Syncs the category hierarchy from ERPNext. **Must be called BEFORE product sync** to ensure categories exist.

### Request Format

```json
{
  "categories": [
    {
      "name": "string (required, category name)",
      "parentName": "string (optional, null = root category)"
    }
  ]
}
```

### Response Format

```json
[
  {
    "id": "integer",
    "name": "string",
    "slug": "string",
    "parent": {
      "id": "integer",
      "name": "string",
      "slug": "string"
    } | null
  }
]
```

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Category display name |
| `parentName` | String | Parent category name (null for root) |

---

## 3. Orders Sync

**Endpoint:** `POST /api/v1/sync/orders`

### Description
Syncs orders from ERPNext. Upserts by `erpOrderId`. Customer is looked up by phone number.

### Request Format

```json
{
  "erpOrderId": "string (required, unique order ID from ERP)",
  "customerPhone": "string (required, for customer lookup)",
  "status": "string (required, order status)",
  "totalAmount": "decimal (required)",
  "items": [
    {
      "erpItemCode": "string (required)",
      "productName": "string (optional)",
      "quantity": "integer (required)",
      "price": "decimal (required)"
    }
  ]
}
```

### Response Format

**Success:**
```json
{
  "synced": 1,
  "errors": 0
}
```

**Skipped (e.g., missing user):**
```json
{
  "synced": 0,
  "errors": 0,
  "skippedReason": "string (reason for skipping)"
}
```

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `erpOrderId` | String | Unique order identifier from ERPNext |
| `customerPhone` | String | Customer phone for lookup |
| `status` | String | Order status (e.g., PENDING, COMPLETED) |
| `totalAmount` | Decimal | Total order amount |
| `erpItemCode` | String | SKU code of ordered item |
| `productName` | String | Display name of product |
| `quantity` | Integer | Quantity ordered |
| `price` | Decimal | Unit price |

---

## 4. Users Sync

### 4.1 Single User Sync

**Endpoint:** `POST /api/v1/sync/users`

### Description
Syncs a single user from ERPNext. Upserts by phone number.

### Request Format

```json
{
  "phone": "string (required, unique identifier)",
  "name": "string (optional)",
  "email": "string (optional)",
  "role": "string (enum: CUSTOMER, ADMIN, etc.)",
  "enabled": "boolean (optional)",
  "loyaltyPoints": "integer (optional)",
  "tierName": "string (optional)",
  "spendToNextTier": "decimal (optional)",
  "spendToMaintainTier": "decimal (optional)",
  "currentMonthSpending": "decimal (optional)"
}
```

### Response Format

```json
{
  "synced": "integer (0 or 1)",
  "errors": "integer (0 or 1)"
}
```

### 4.2 Batch User Sync

**Endpoint:** `POST /api/v1/sync/users/batch`

### Request Format

```json
[
  {
    "phone": "string (required)",
    "name": "string",
    "email": "string",
    "role": "string",
    "enabled": "boolean",
    "loyaltyPoints": "integer",
    "tierName": "string",
    "spendToNextTier": "decimal",
    "spendToMaintainTier": "decimal",
    "currentMonthSpending": "decimal"
  }
]
```

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `phone` | String | Unique user identifier (phone number) |
| `name` | String | User's full name |
| `email` | String | User's email address |
| `role` | UserRole | User role (CUSTOMER, ADMIN, etc.) |
| `enabled` | Boolean | Account enabled status |
| `loyaltyPoints` | Integer | Current loyalty points balance |
| `tierName` | String | Current loyalty tier name |
| `spendToNextTier` | Decimal | Amount needed to reach next tier |
| `spendToMaintainTier` | Decimal | Amount needed to maintain current tier |
| `currentMonthSpending` | Decimal | Spending in current month |

---

## 5. Loyalty Points Sync

**Endpoint:** `POST /api/v1/sync/loyalty`

### Description
Syncs loyalty points for a user, looked up by phone number.

### Request Format

```json
{
  "phone": "string (required, unique identifier)",
  "points": "integer (required, must be >= 0)",
  "tierName": "string (optional)",
  "spendToNextTier": "decimal (optional)",
  "spendToMaintainTier": "decimal (optional)",
  "currentMonthSpending": "decimal (optional)"
}
```

### Response Format

**Success:** `204 No Content`

**Error:** `500 Internal Server Error`

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `phone` | String | User's phone number (lookup key) |
| `points` | Integer | Loyalty points balance |
| `tierName` | String | Current loyalty tier |
| `spendToNextTier` | Decimal | Spending required for next tier |
| `spendToMaintainTier` | Decimal | Spending required to maintain tier |
| `currentMonthSpending` | Decimal | Current month's spending |

---

## 6. Loyalty Tiers Sync

**Endpoint:** `POST /api/v1/sync/loyalty-tiers`

### Description
Syncs loyalty tier definitions from ERPNext.

### Request Format

```json
[
  {
    "name": "string (required, unique tier name)",
    "discountPercentage": "decimal (required, >= 0)",
    "cashbackPercentage": "decimal (required, >= 0)",
    "minSpendRequirement": "decimal (required, >= 0)",
    "displayOrder": "integer (required)",
    "color": "string (optional, hex color code)"
  }
]
```

### Response Format

**Success:** `204 No Content`

**Error:** `500 Internal Server Error`

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Tier name (e.g., "Gold", "Silver") |
| `discountPercentage` | Decimal | Member discount percentage |
| `cashbackPercentage` | Decimal | Cashback percentage |
| `minSpendRequirement` | Decimal | Minimum spending to qualify |
| `displayOrder` | Integer | Display order in UI |
| `color` | String | Hex color code for UI display |

---

## 7. Discounts Sync (Pricing Rules)

**Endpoint:** `POST /api/v1/sync/discounts`

### Description
Syncs Pricing Rule discounts from ERPNext. Upserts by `erpPricingRuleId`. One pricing rule may cover multiple SKU item codes.

### Request Format

```json
{
  "erpPricingRuleId": "string (required, unique rule ID)",
  "itemCodes": ["string array (required, list of SKU codes)"],
  "discountValue": "decimal (required, 0.00 - 100.00)",
  "validFrom": "ISO 8601 timestamp (required)",
  "validUpto": "ISO 8601 timestamp (required)",
  "disabled": "boolean (required)",
  "title": "string (optional, display title)",
  "colorHex": "string (optional, for UI display)"
}
```

### Response Format

**Success:** `204 No Content`

**Error:** `500 Internal Server Error`

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `erpPricingRuleId` | String | Unique pricing rule ID from ERPNext |
| `itemCodes` | String[] | List of SKU codes this discount applies to |
| `discountValue` | Decimal | Discount percentage (0-100) |
| `validFrom` | Instant | Start date/time of discount validity |
| `validUpto` | Instant | End date/time of discount validity |
| `disabled` | Boolean | Whether the rule is disabled |
| `title` | String | Display title for the discount |
| `colorHex` | String | Hex color code for UI display |

---

## 8. Remove Discount

**Endpoint:** `DELETE /api/v1/sync/discounts`

### Description
Handles the `on_trash` signal from ERPNext — permanently deletes the Pricing Rule discount.

### Request Format

```json
{
  "erpPricingRuleId": "string (required)"
}
```

### Response Format

**Success:** `204 No Content`

**Error:** `500 Internal Server Error`

---

## Idempotency Handling

The following endpoints require an `Idempotency-Key` header:

- `/products`
- `/orders`
- `/loyalty`
- `/loyalty-tiers`
- `/discounts` (POST and DELETE)

### Header Format
```
Idempotency-Key: <unique-key-per-request>
```

### Behavior
- **First request:** Processed normally, key stored with event type and status code
- **Duplicate key:** Returns `409 Conflict` with error message
- **Key format:** Client-generated unique identifier (UUID recommended)

### Event Types Tracked
| Event Type | Endpoint |
|------------|----------|
| `PRODUCT` | `/products` |
| `ORDER` | `/orders` |
| `LOYALTY` | `/loyalty` |
| `LOYALTY_TIER` | `/loyalty-tiers` |
| `DISCOUNT` | `/discounts` |
| `USER` | `/users` (not tracked) |
| `CATEGORIES` | `/categories` (not tracked) |

---

## Sync Logging

All sync operations are logged to the `erp_sync_log` table:

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | Auto-increment primary key |
| `erp_id` | String (64) | Identifier of synced entity |
| `synced_at` | Timestamp | Sync timestamp |
| `status` | String (16) | `SUCCESS` or `ERROR` |
| `error_message` | TEXT | Error details (if failed) |

---

## Security

- **Authentication:** API Key (JWT-based)
- **Authorization:** `SYSTEM` role required for all sync endpoints
- **Rate Limiting:** Not configured (relies on ERPNext webhook throttling)

---

## Batch Processing

A scheduled job (`ErpSyncScheduler`) runs periodic full syncs using Spring Batch:
- **Job:** `erpInitialImportJob`
- **Chunk Size:** 50 items
- **Processor:** Converts ERP snapshots to sync commands
- **Writer:** Calls `SyncProductHierarchyUseCase` for each command

---

## Error Handling

### Response Codes
| Code | Meaning |
|------|---------|
| 200 | Success with result body |
| 204 | Success with no content |
| 400 | Bad request (e.g., missing Idempotency-Key) |
| 409 | Duplicate idempotency key |
| 422 | Unprocessable entity (e.g., order skipped) |
| 500 | Internal server error |

### Error Response Format
```json
{
  "type": "ERROR",
  "message": "Error description"
}
```

---

## Data Flow Summary

```
ERPNext (Source of Truth)
    │
    ├── Webhooks ──→ /api/v1/sync/* ──→ Domain Services ──→ Database
    │
    └── Scheduled Batch ──→ JobLauncher ──→ Spring Batch Pipeline ──→ Database
```

### Sync Order Dependencies
1. **Categories** (must be first)
2. **Loyalty Tiers** (before users/loyalty points)
3. **Users** (before orders)
4. **Products** (independent, but after categories)
5. **Orders** (requires users and products)
6. **Loyalty Points** (requires users and tiers)
7. **Discounts** (independent, applies to products)

---

## Recommendations

1. **Always sync categories before products** to avoid orphaned product categories
2. **Use UUIDs for Idempotency-Key** to prevent collisions
3. **Monitor `erp_sync_log` table** for failed syncs
4. **Implement retry logic** with exponential backoff for 5xx errors
5. **Validate data consistency** between ERPNext and backend periodically

---

*Report generated by scanning backend source code in `/backend/src/main/java/tj/radolfa/`*
