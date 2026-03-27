# Radolfa Backend — Frontend Endpoints Reference

> Updated: 2026-03-20
> Base URL: `/api/v1`
> Auth: `Authorization: Bearer <accessToken>` header **or** HTTP-only cookie `authToken`

---

## Legend

- 🌐 **Public** — No authentication required
- 👤 **USER+** — Requires any authenticated user (`USER`, `MANAGER`, `ADMIN`)
- 🔧 **MANAGER+** — Requires `MANAGER` or `ADMIN` role
- 🔑 **ADMIN** — Requires `ADMIN` role only

---

## 1. Authentication (`/api/v1/auth`)

All auth endpoints are public unless noted.

---

### `POST /api/v1/auth/login` 🌐

Send a one-time password to a phone number.

**Request Body**
```json
{
  "phone": "+992901234567"
}
```

**Response `200 OK`**
```json
{
  "message": "OTP sent",
  "success": true
}
```

**Notes:**
- OTP is 4–6 digits, expires in 5 minutes.
- Rate-limited by IP and by phone number.

---

### `POST /api/v1/auth/verify` 🌐

Verify OTP and receive JWT tokens. Automatically registers new users on first login.

**Request Body**
```json
{
  "phone": "+992901234567",
  "otp": "1234"
}
```

**Response `200 OK`**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "phone": "+992901234567",
    "name": "Alijon",
    "email": "ali@example.com",
    "role": "USER",
    "enabled": true,
    "loyalty": { /* LoyaltyDto — see §9 */ }
  }
}
```

**Notes:**
- Access token expires in 15 minutes.
- Refresh token expires in 7 days (stored as HTTP-only cookie).
- Rate-limited: max 5 verification attempts per phone per 15 minutes.
- New phone = account auto-created with `USER` role.

---

### `POST /api/v1/auth/refresh` 🌐

Exchange the refresh token cookie for a new access token.

**Request Body**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response `200 OK`**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

---

### `GET /api/v1/auth/me` 👤 USER+

Get the currently authenticated user's profile (quick identity check).

**Response `200 OK`** — `UserDto` (same shape as the `user` object in `/verify`).

---

### `POST /api/v1/auth/logout` 👤 USER+

Clear authentication cookies server-side.

**Response `200 OK`**
```json
{ "message": "Logged out", "success": true }
```

---

## 2. Listings (Product Catalog) (`/api/v1/listings`)

Read operations are public. Write operations require MANAGER+.

> **Important:** Write endpoints use `{slug}`, not a numeric ID.

---

### `GET /api/v1/listings` 🌐

Browse listings with filters. Returns paginated results with tier-enriched pricing.

**Query Parameters**

| Param | Type | Description |
|---|---|---|
| `q` | string | Full-text search query |
| `categoryId` | Long | Filter by category |
| `colorKey` | string | Filter by color key |
| `minPrice` | BigDecimal | Minimum price |
| `maxPrice` | BigDecimal | Maximum price |
| `topSelling` | boolean | Filter top-selling variants |
| `featured` | boolean | Filter featured variants |
| `page` | int | Page number (0-based, default 0) |
| `size` | int | Page size (default 20) |
| `sort` | string | Sort field (e.g., `price,asc`) |

**Response `200 OK`**
```json
{
  "content": [
    {
      "variantId": 10,
      "productCode": "RAD-0001",
      "slug": "black-hoodie-xl",
      "colorKey": "BLACK",
      "colorDisplayName": "Black",
      "colorHex": "#000000",
      "categoryName": "Hoodies",
      "topSelling": true,
      "featured": false,
      "images": ["https://s3.twcstorage.ru/.../main.jpg"],
      "minPrice": 150.00,
      "maxPrice": 180.00,
      "tierDiscountedMinPrice": 142.50,
      "skus": [
        {
          "skuId": 101,
          "skuCode": "RAD-0001-BLACK-M",
          "sizeLabel": "M",
          "stockQuantity": 15,
          "price": 150.00
        }
      ]
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

### `GET /api/v1/listings/search` 🌐

Full-text search via Elasticsearch (falls back to SQL if ES is unavailable).

**Query Parameters**

| Param | Type | Description |
|---|---|---|
| `q` | string | Search query (required) |
| `page` | int | Page number (0-based) |
| `size` | int | Page size |

**Response `200 OK`** — Same paginated shape as `GET /listings`.

---

### `GET /api/v1/listings/autocomplete` 🌐

Search suggestions for a type-ahead input.

**Query Parameters**

| Param | Type | Description |
|---|---|---|
| `q` | string | Partial search query |

**Response `200 OK`**
```json
["Classic Hoodie", "Classic Tee", "Classic Parka"]
```

---

### `GET /api/v1/listings/{slug}` 🌐

Get full variant detail including SKUs, sibling color variants, and attributes.

**Path Parameters**

| Param | Description |
|---|---|
| `slug` | URL-safe variant identifier (e.g., `black-hoodie-xl`) |

**Response `200 OK`**
```json
{
  "variantId": 10,
  "productCode": "RAD-0001",
  "productBaseId": 5,
  "slug": "black-hoodie-xl",
  "colorKey": "BLACK",
  "colorDisplayName": "Black",
  "colorHex": "#000000",
  "categoryId": 3,
  "categoryName": "Hoodies",
  "webDescription": "<p>Premium quality hoodie...</p>",
  "images": [
    "https://s3.twcstorage.ru/.../1.jpg",
    "https://s3.twcstorage.ru/.../2.jpg"
  ],
  "attributes": [
    { "key": "Material", "value": "100% Cotton", "sortOrder": 1 },
    { "key": "Fit", "value": "Oversized", "sortOrder": 2 }
  ],
  "topSelling": true,
  "featured": false,
  "minPrice": 150.00,
  "maxPrice": 180.00,
  "tierDiscountedMinPrice": 142.50,
  "skus": [
    {
      "skuId": 101,
      "skuCode": "RAD-0001-BLACK-M",
      "sizeLabel": "M",
      "stockQuantity": 15,
      "price": 150.00
    },
    {
      "skuId": 102,
      "skuCode": "RAD-0001-BLACK-L",
      "sizeLabel": "L",
      "stockQuantity": 8,
      "price": 165.00
    }
  ]
}
```

---

### `PUT /api/v1/listings/{slug}` 🔧 MANAGER+

Update listing enrichment fields. **Does not update price or stock.**

**Request Body**
```json
{
  "webDescription": "<p>Updated description...</p>",
  "attributes": [
    { "key": "Material", "value": "80% Cotton, 20% Polyester", "sortOrder": 1 }
  ],
  "topSelling": true,
  "featured": false
}
```

**Response `200 OK`** — Updated listing detail (same shape as `GET /{slug}`).

---

### `POST /api/v1/listings/{slug}/images` 🔧 MANAGER+

Upload images for a listing. Java resizes and stores in S3.

**Request** — `multipart/form-data`

| Field | Type | Description |
|---|---|---|
| `files` | File[] | Image files (JPEG, PNG) |

**Response `200 OK`**
```json
{
  "images": [
    "https://s3.twcstorage.ru/.../img_800.jpg",
    "https://s3.twcstorage.ru/.../img_400.jpg",
    "https://s3.twcstorage.ru/.../img_150.jpg"
  ]
}
```

**Notes:**
- Server produces three sizes: 150×150, 400×400, 800×800.
- Frontend must always use S3 URLs — no local image processing.

---

### `DELETE /api/v1/listings/{slug}/images` 🔧 MANAGER+

Remove an image from a listing.

**Request Body**
```json
{
  "imageUrl": "https://s3.twcstorage.ru/.../img_800.jpg"
}
```

**Response `204 No Content`**

---

## 3. Home & Collections (`/api/v1/home`)

All public.

---

### `GET /api/v1/home/collections` 🌐

Returns ordered homepage sections (Featured, New Arrivals, Deals) with tier-enriched pricing.

**Response `200 OK`**
```json
{
  "sections": [
    {
      "title": "Featured",
      "key": "featured",
      "listings": [ /* ListingVariantDto[] */ ]
    },
    {
      "title": "New Arrivals",
      "key": "new-arrivals",
      "listings": [ /* ListingVariantDto[] */ ]
    },
    {
      "title": "Deals",
      "key": "deals",
      "listings": [ /* ListingVariantDto[] */ ]
    }
  ]
}
```

---

### `GET /api/v1/home/collections/{key}` 🌐

Get a paginated single collection by its key.

**Response `200 OK`**
```json
{
  "title": "New Arrivals",
  "key": "new-arrivals",
  "listings": [ /* ListingVariantDto[] */ ]
}
```

---

## 4. Categories (`/api/v1/categories`)

---

### `GET /api/v1/categories` 🌐

Get the full category tree.

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "name": "Clothing",
    "slug": "clothing",
    "parentId": null,
    "children": [
      {
        "id": 2,
        "name": "Hoodies",
        "slug": "hoodies",
        "parentId": 1,
        "children": []
      }
    ]
  }
]
```

---

### `GET /api/v1/categories/{slug}/products` 🌐

Get paginated listings belonging to a category (includes descendant categories).

**Path Parameters**

| Param | Description |
|---|---|
| `slug` | Category slug |

**Query Parameters:** `page`, `size`, `sort` — same as `GET /listings`.

**Response `200 OK`** — Same paginated shape as `GET /listings`.

---

## 5. Cart (`/api/v1/cart`)

All endpoints require authentication (`USER+`).

---

### `GET /api/v1/cart` 👤 USER+

Get the current user's active cart.

**Response `200 OK`**
```json
{
  "cartId": 42,
  "items": [
    {
      "skuId": 101,
      "skuCode": "RAD-0001-BLACK-M",
      "productName": "Classic Hoodie",
      "colorName": "Black",
      "sizeLabel": "M",
      "imageUrl": "https://s3.twcstorage.ru/.../img_150.jpg",
      "unitPrice": 150.00,
      "quantity": 2,
      "lineTotal": 300.00,
      "availableStock": 15,
      "inStock": true
    }
  ],
  "totalAmount": 300.00,
  "itemCount": 2
}
```

**Notes:**
- `availableStock` and `inStock` reflect live stock at read time — use these to warn the user before checkout.

---

### `POST /api/v1/cart/items` 👤 USER+

Add an item to the cart. If the SKU already exists, quantities are merged.

**Request Body**
```json
{
  "skuId": 101,
  "quantity": 2
}
```

**Response `200 OK`** — Updated cart (same shape as `GET /cart`).

**Errors:**
- `404` — SKU not found.
- `400` — Insufficient stock.

---

### `PUT /api/v1/cart/items/{skuId}` 👤 USER+

Update quantity of a cart item. Passing `quantity <= 0` removes the item.

**Request Body**
```json
{
  "quantity": 3
}
```

**Response `200 OK`** — Updated cart.

---

### `DELETE /api/v1/cart/items/{skuId}` 👤 USER+

Remove a specific item from the cart.

**Response `200 OK`** — Updated cart.

---

### `DELETE /api/v1/cart` 👤 USER+

Clear all items from the cart.

**Response `204 No Content`**

---

## 6. Orders (`/api/v1/orders`)

---

### `POST /api/v1/orders/checkout` 👤 USER+

Convert the current cart into a `PENDING` order. Deducts stock and redeems loyalty points at this point.

**Request Body**
```json
{
  "loyaltyPointsToRedeem": 50,
  "notes": "Leave at the door"
}
```

Pass `0` for `loyaltyPointsToRedeem` to skip loyalty redemption. `notes` is optional.

**Response `201 Created`**
```json
{
  "orderId": 99,
  "status": "PENDING",
  "subtotal": 300.00,
  "tierDiscount": 15.00,
  "pointsDiscount": 50.00,
  "total": 235.00
}
```

**Errors:**
- `400` — Cart is empty.
- `400` — Insufficient stock (re-checked at checkout).
- `400` — Insufficient loyalty points.

---

### `GET /api/v1/orders/my-orders` 👤 USER+

Get the current user's order history.

**Response `200 OK`**
```json
[
  {
    "id": 99,
    "status": "PAID",
    "totalAmount": 235.00,
    "createdAt": "2026-03-15T10:30:00Z",
    "items": [
      {
        "productName": "Classic Hoodie",
        "quantity": 2,
        "price": 150.00
      }
    ]
  }
]
```

---

### `GET /api/v1/orders/{orderId}` 👤 USER+

Get a specific order. Users can only fetch their own orders.

**Response `200 OK`** — Single `OrderDto` (same shape as items in `my-orders`).

---

### `PATCH /api/v1/orders/{orderId}/cancel` 👤 USER+ / 🔑 ADMIN

Cancel an order.

- `USER` — can only cancel their own `PENDING` orders.
- `ADMIN` — can cancel any order that is not in a final state.

**Response `200 OK`** — Updated `OrderDto`.

---

### `PATCH /api/v1/orders/{orderId}/status` 🔑 ADMIN

Update order status (admin/operations use).

**Request Body**
```json
{
  "status": "SHIPPED"
}
```

Valid transitions: `PENDING → PAID → SHIPPED → DELIVERED`.

**Response `200 OK`** — Updated `OrderDto`.

---

## 7. Payments (`/api/v1/payments`)

---

### `POST /api/v1/payments/initiate/{orderId}` 👤 USER+

Initiate payment for an order the user owns. Returns a redirect URL to the payment provider.

**Path Parameters**

| Param | Description |
|---|---|
| `orderId` | ID of the `PENDING` order to pay |

**Response `200 OK`**
```json
{
  "paymentId": 55,
  "redirectUrl": "https://payment-provider.com/pay/xyz123"
}
```

---

### `GET /api/v1/payments/{orderId}` 🌐

Get payment status for an order (poll after redirect back from the payment provider).

**Response `200 OK`**
```json
{
  "paymentId": 55,
  "status": "COMPLETED",
  "provider": "payme",
  "amount": 235.00
}
```

Status values: `PENDING`, `COMPLETED`, `REFUNDED`.

---

### `POST /api/v1/payments/{orderId}/refund` 🔑 ADMIN

Refund a payment. Reverses loyalty points and restores stock.

**Response `200 OK`**
```json
{
  "paymentId": 55,
  "status": "REFUNDED",
  "provider": "payme",
  "amount": 235.00
}
```

---

## 8. Payment Webhook (`/api/v1/webhooks`)

Called by the external payment provider. **Not called by the frontend.**

---

### `POST /api/v1/webhooks/payment` 🌐

Payment provider confirms a successful payment.

**Query Parameters**

| Param | Description |
|---|---|
| `transactionId` | Provider-assigned transaction ID |

**Request Body** — Raw string payload (provider-specific format).

**Response `200 OK`**

**Notes:**
- Idempotent — safe to call multiple times.
- Triggers loyalty point award after confirmation.
- Signature/integrity validated inside the use case.

---

## 9. User Profile (`/api/v1/users`)

---

### `GET /api/v1/users/me` 👤 USER+

Get the current user's full profile including loyalty status.

**Response `200 OK`**
```json
{
  "id": 1,
  "phone": "+992901234567",
  "name": "Alijon",
  "email": "ali@example.com",
  "role": "USER",
  "enabled": true,
  "loyalty": {
    "points": 120,
    "tier": {
      "id": 2,
      "name": "Gold",
      "discountPercentage": 5.0,
      "cashbackPercentage": 5.0,
      "minSpendRequirement": 1000.00,
      "displayOrder": 2,
      "color": "#FFD700"
    },
    "spendToNextTier": 500.00,
    "spendToMaintainTier": 200.00,
    "currentMonthSpending": 750.00,
    "recentEarnings": [
      {
        "orderId": 99,
        "pointsEarned": 12,
        "orderAmount": 235.00,
        "orderedAt": "2026-03-15T10:30:00Z"
      }
    ]
  }
}
```

---

### `PUT /api/v1/users/profile` 👤 USER+

Update own profile (name and email only).

**Request Body**
```json
{
  "name": "Alijon Karimov",
  "email": "alijon@example.com"
}
```

**Response `200 OK`** — Updated `UserDto`.

**Errors:**
- `409` — Email already in use by another account.

---

### `GET /api/v1/users` 🔧 MANAGER+

List all users (admin panel).

**Query Parameters:** `page`, `size`, `q` (search by name or phone).

**Response `200 OK`** — Paginated list of `UserDto`.

---

### `PATCH /api/v1/users/{userId}/role` 🔑 ADMIN

Change a user's role.

**Request Body**
```json
{
  "role": "MANAGER"
}
```

Valid roles: `USER`, `MANAGER`, `ADMIN`.

**Response `200 OK`** — Updated `UserDto`.

---

### `PATCH /api/v1/users/{userId}/status` 🔧 MANAGER+

Enable or disable a user account.

**Request Body**
```json
{
  "enabled": false
}
```

**Response `200 OK`** — Updated `UserDto`.

---

## 10. Product Management (`/api/v1/admin`)

---

### `POST /api/v1/admin/products` 🔧 MANAGER+

Create a new product with one color variant and its SKUs.

**Request Body**
```json
{
  "name": "Classic Hoodie",
  "categoryId": 2,
  "colorId": 3,
  "skus": [
    { "sizeLabel": "S", "price": 140.00, "stockQuantity": 20 },
    { "sizeLabel": "M", "price": 150.00, "stockQuantity": 30 },
    { "sizeLabel": "L", "price": 160.00, "stockQuantity": 25 }
  ]
}
```

**Notes:**
- One product creation call = one color variant. To add another color, create a second variant separately or use a future batch endpoint.
- `price` and `stockQuantity` accept `0` (PositiveOrZero).

**Response `201 Created`**
```json
{
  "productBaseId": 5,
  "variantId": 10,
  "slug": "classic-hoodie-black"
}
```

---

### `PUT /api/v1/admin/skus/{skuId}/price` 🔑 ADMIN

Update the price of a SKU. **ADMIN only — MANAGER cannot do this.**

**Request Body**
```json
{
  "price": 165.00
}
```

**Response `200 OK`**
```json
{
  "skuId": 101,
  "skuCode": "RAD-0001-BLACK-M",
  "price": 165.00
}
```

---

### `PUT /api/v1/admin/skus/{skuId}/stock` 🔑 ADMIN

Set or adjust stock quantity. **ADMIN only.**

**Request Body (absolute set)**
```json
{
  "quantity": 50
}
```

**Request Body (relative adjustment)**
```json
{
  "delta": -5
}
```

Provide exactly one of `quantity` (≥0) or `delta` (signed integer).

**Response `200 OK`**
```json
{
  "skuId": 101,
  "skuCode": "RAD-0001-BLACK-M",
  "stockQuantity": 50
}
```

---

## 11. Categories Management (`/api/v1/admin/categories`)

---

### `POST /api/v1/admin/categories` 🔧 MANAGER+

Create a new category.

**Request Body**
```json
{
  "name": "Outerwear",
  "parentId": 1
}
```

Pass `null` for `parentId` to create a root category.

**Response `201 Created`**
```json
{
  "id": 10,
  "name": "Outerwear",
  "parentId": 1
}
```

---

### `DELETE /api/v1/admin/categories/{categoryId}` 🔑 ADMIN

Delete a category. Fails if any products are still assigned to it.

**Response `204 No Content`**

**Errors:**
- `422` — Category is in use by one or more products.

---

## 12. Loyalty Tiers (`/api/v1/loyalty-tiers`)

Public read. Only the tier color can be updated (MANAGER+). Full tier CRUD is backend-managed.

---

### `GET /api/v1/loyalty-tiers` 🌐

List all loyalty tiers ordered by `displayOrder`. Use this to display tier badges and benefits.

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "name": "Silver",
    "discountPercentage": 3.0,
    "cashbackPercentage": 3.0,
    "minSpendRequirement": 500.00,
    "displayOrder": 1,
    "color": "#C0C0C0"
  },
  {
    "id": 2,
    "name": "Gold",
    "discountPercentage": 5.0,
    "cashbackPercentage": 5.0,
    "minSpendRequirement": 1000.00,
    "displayOrder": 2,
    "color": "#FFD700"
  }
]
```

---

### `PATCH /api/v1/loyalty-tiers/{tierId}/color` 🔧 MANAGER+

Update the display color of a loyalty tier.

**Request Body**
```json
{
  "color": "#E5E4E2"
}
```

**Response `200 OK`** — Updated `LoyaltyTierDto`.

---

## 13. Colors (`/api/v1/colors`)

---

### `GET /api/v1/colors` 🌐

List all available colors.

**Response `200 OK`**
```json
[
  { "id": 1, "colorKey": "BLACK", "displayName": "Black", "hexCode": "#000000" },
  { "id": 2, "colorKey": "WHITE", "displayName": "White", "hexCode": "#FFFFFF" }
]
```

---

### `PATCH /api/v1/colors/{colorId}` 🔧 MANAGER+

Update a color's display name or hex code.

**Request Body**
```json
{
  "displayName": "Jet Black",
  "hexCode": "#0A0A0A"
}
```

**Response `200 OK`** — Updated `ColorDto`.

---

## 14. Search (`/api/v1/search`)

---

### `POST /api/v1/search/reindex` 🔑 ADMIN

Rebuild the Elasticsearch index from PostgreSQL. Run after bulk data imports.

**Response `200 OK`**
```json
{
  "indexed": 1250,
  "errorCount": 0,
  "message": "Reindex complete"
}
```

---

## Common Response Patterns

### Error Responses

```json
{
  "timestamp": "2026-03-20T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient stock for SKU RAD-0001-BLACK-M",
  "path": "/api/v1/cart/items"
}
```

| Status | Meaning |
|---|---|
| `400` | Validation error or business rule violation |
| `401` | Missing or invalid JWT |
| `403` | Insufficient role or field locked |
| `404` | Resource not found |
| `409` | Conflict (e.g., duplicate email, optimistic lock) |
| `422` | Unprocessable state (e.g., image processing failed, category in use) |
| `500` | Internal server error |

### Message Response

Simple operations return a `MessageResponseDto`:

```json
{ "message": "OTP sent", "success": true }
```

### Pagination Response Wrapper

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "number": 0,
  "size": 20,
  "first": true,
  "last": true
}
```

---

## Frontend Quick Reference

| Feature | Method | Endpoint |
|---|---|---|
| Send OTP | POST | `/auth/login` |
| Verify OTP / Login | POST | `/auth/verify` |
| Refresh token | POST | `/auth/refresh` |
| Current user (quick) | GET | `/auth/me` |
| Logout | POST | `/auth/logout` |
| My profile + loyalty | GET | `/users/me` |
| Update profile | PUT | `/users/profile` |
| Product grid | GET | `/listings` |
| Full-text search | GET | `/listings/search` |
| Search autocomplete | GET | `/listings/autocomplete` |
| Product detail | GET | `/listings/{slug}` |
| Homepage sections | GET | `/home/collections` |
| Single collection | GET | `/home/collections/{key}` |
| Category tree | GET | `/categories` |
| Category products | GET | `/categories/{slug}/products` |
| All colors | GET | `/colors` |
| All loyalty tiers | GET | `/loyalty-tiers` |
| Cart | GET | `/cart` |
| Add to cart | POST | `/cart/items` |
| Update cart item | PUT | `/cart/items/{skuId}` |
| Remove cart item | DELETE | `/cart/items/{skuId}` |
| Clear cart | DELETE | `/cart` |
| Checkout | POST | `/orders/checkout` |
| My orders | GET | `/orders/my-orders` |
| Order detail | GET | `/orders/{orderId}` |
| Cancel order | PATCH | `/orders/{orderId}/cancel` |
| Initiate payment | POST | `/payments/initiate/{orderId}` |
| Payment status poll | GET | `/payments/{orderId}` |
| Upload listing images | POST | `/listings/{slug}/images` |
| Update listing content | PUT | `/listings/{slug}` |
| Create product | POST | `/admin/products` |
| Update price (ADMIN) | PUT | `/admin/skus/{id}/price` |
| Update stock (ADMIN) | PUT | `/admin/skus/{id}/stock` |
| List users | GET | `/users` |
| Change user role | PATCH | `/users/{id}/role` |
| Block/unblock user | PATCH | `/users/{id}/status` |
| Create category | POST | `/admin/categories` |
| Delete category | DELETE | `/admin/categories/{id}` |
| Update tier color | PATCH | `/loyalty-tiers/{id}/color` |
| Update color | PATCH | `/colors/{id}` |
| Reindex search | POST | `/search/reindex` |
