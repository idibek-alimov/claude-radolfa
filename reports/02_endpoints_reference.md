# Radolfa Backend — Frontend Endpoints Reference

> Generated: 2026-03-20
> Base URL: `/api/v1`
> Auth: `Authorization: Bearer <accessToken>` or HTTP-only cookie `authToken`

---

## Legend

- 🌐 **Public** — No authentication required
- 👤 **USER+** — Requires any authenticated user (`USER`, `MANAGER`, `ADMIN`)
- 🔧 **MANAGER+** — Requires `MANAGER` or `ADMIN` role
- 🔑 **ADMIN** — Requires `ADMIN` role only

---

## 1. Authentication (`/api/v1/auth`)

All endpoints are public (unauthenticated).

---

### `POST /api/v1/auth/otp/send` 🌐

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
  "message": "OTP sent"
}
```

**Notes:**
- OTP is 4 digits, expires in 5 minutes.
- Rate-limited: max 5 requests per phone per 60 minutes.

---

### `POST /api/v1/auth/otp/verify` 🌐

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
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "phone": "+992901234567",
    "name": "Alijon",
    "email": "ali@example.com",
    "role": "USER",
    "enabled": true
  }
}
```

**Notes:**
- Access token expires in 15 minutes.
- Refresh token expires in 7 days.
- Rate-limited: max 5 verification attempts per phone per 15 minutes.
- New phone = account auto-created with `USER` role.

---

### `POST /api/v1/auth/refresh` 🌐

Exchange a refresh token for a new access token.

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
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

## 2. Listings (Product Catalog) (`/api/v1/listings`)

Read operations are public. Write operations require MANAGER+.

---

### `GET /api/v1/listings` 🌐

Search and browse listings with filters. Returns paginated results.

**Query Parameters**

| Param | Type | Description |
|---|---|---|
| `q` | string | Full-text search query |
| `categoryId` | Long | Filter by category |
| `colorKey` | string | Filter by color |
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
  "size": 20
}
```

---

### `GET /api/v1/listings/{slug}` 🌐

Get a single listing by its slug (used for product detail page).

**Path Parameters**

| Param | Description |
|---|---|
| `slug` | URL-safe product variant identifier |

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

### `PUT /api/v1/listings/{listingId}` 🔧 MANAGER+

Update listing enrichment (description, attributes, flags). **Does not update price or stock.**

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

**Response `200 OK`** — Updated `ListingVariantDetailDto` (same shape as GET /{slug}).

---

### `POST /api/v1/listings/{listingId}/images` 🔧 MANAGER+

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
- Frontend must use S3 URLs — no local image processing.

---

### `DELETE /api/v1/listings/{listingId}/images` 🔧 MANAGER+

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

All public. Used for the homepage and collection pages.

---

### `GET /api/v1/home` 🌐

Returns data for the homepage: featured sections, top-selling products.

**Response `200 OK`**
```json
{
  "sections": [
    {
      "title": "New Arrivals",
      "slug": "new-arrivals",
      "listings": [ /* ListingVariantDto[] */ ]
    },
    {
      "title": "Top Selling",
      "slug": "top-selling",
      "listings": [ /* ListingVariantDto[] */ ]
    }
  ]
}
```

---

### `GET /api/v1/home/collections/{slug}` 🌐

Get a specific collection page.

**Response `200 OK`**
```json
{
  "title": "Summer Collection",
  "slug": "summer-2026",
  "listings": [ /* ListingVariantDto[] */ ]
}
```

---

## 4. Categories (`/api/v1/categories`)

---

### `GET /api/v1/categories` 🌐

Get full category tree.

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "name": "Clothing",
    "parentId": null,
    "children": [
      {
        "id": 2,
        "name": "Hoodies",
        "parentId": 1,
        "children": []
      }
    ]
  }
]
```

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
      "colorKey": "BLACK",
      "sizeLabel": "M",
      "image": "https://s3.twcstorage.ru/.../img_150.jpg",
      "unitPrice": 150.00,
      "quantity": 2,
      "lineTotal": 300.00
    }
  ],
  "totalAmount": 300.00,
  "itemCount": 2
}
```

---

### `POST /api/v1/cart/items` 👤 USER+

Add an item to the cart.

**Request Body**
```json
{
  "skuId": 101,
  "quantity": 2
}
```

**Response `200 OK`** — Updated cart (same shape as GET /cart).

**Errors:**
- `404` — SKU not found.
- `400` — Insufficient stock.

---

### `PUT /api/v1/cart/items/{skuId}` 👤 USER+

Update quantity of a cart item.

**Request Body**
```json
{
  "quantity": 3
}
```

**Response `200 OK`** — Updated cart.

---

### `DELETE /api/v1/cart/items/{skuId}` 👤 USER+

Remove an item from the cart.

**Response `200 OK`** — Updated cart.

---

### `DELETE /api/v1/cart` 👤 USER+

Clear all items from the cart.

**Response `204 No Content`**

---

## 6. Orders (`/api/v1/orders`)

---

### `POST /api/v1/orders/checkout` 👤 USER+

Convert the current cart into an order. Deducts stock and points at this point.

**Request Body**
```json
{
  "loyaltyPointsToRedeem": 50
}
```
Pass `0` to skip loyalty redemption.

**Response `201 Created`**
```json
{
  "orderId": 99,
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

### `GET /api/v1/orders` 👤 USER+

Get the current user's order history.

**Response `200 OK`**
```json
[
  {
    "orderId": 99,
    "status": "PAID",
    "totalAmount": 235.00,
    "createdAt": "2026-03-15T10:30:00Z",
    "items": [
      {
        "skuCode": "RAD-0001-BLACK-M",
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

**Response `200 OK`** — Single `OrderDto` (same shape as items above).

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

### `DELETE /api/v1/orders/{orderId}` 👤 USER+

Cancel an order. Only possible when status is `PENDING`.

**Response `204 No Content`**

---

## 7. Payments (`/api/v1/payments`)

---

### `POST /api/v1/payments/initiate` 👤 USER+

Initiate payment for an order. Returns a payment URL to redirect the user.

**Request Body**
```json
{
  "orderId": 99
}
```

**Response `200 OK`**
```json
{
  "paymentId": 55,
  "paymentUrl": "https://payment-provider.com/pay/xyz123",
  "status": "PENDING"
}
```

---

### `GET /api/v1/payments/{paymentId}/status` 👤 USER+

Check payment status (poll after redirect back from payment provider).

**Response `200 OK`**
```json
{
  "paymentId": 55,
  "orderId": 99,
  "status": "COMPLETED",
  "message": "Payment successful"
}
```

Status values: `PENDING`, `COMPLETED`, `REFUNDED`.

---

### `POST /api/v1/payments/{paymentId}/refund` 🔑 ADMIN

Refund a payment. Reverses loyalty points.

**Response `200 OK`**
```json
{
  "paymentId": 55,
  "status": "REFUNDED"
}
```

---

## 8. Payment Webhook (`/api/v1/webhooks/payment`)

Called by the external payment provider. Not called by the frontend directly.

---

### `POST /api/v1/webhooks/payment/confirm`

Payment provider confirms a successful payment.

**Request Body** (provider-specific, example)
```json
{
  "providerTransactionId": "txn_abc123",
  "status": "SUCCESS"
}
```

**Response `200 OK`**

**Notes:**
- Idempotent — safe to call multiple times.
- Triggers loyalty point award after confirmation.
- Protected by `X-Api-Key` header (system key).

---

## 9. User Profile (`/api/v1/users`)

---

### `GET /api/v1/users/me` 👤 USER+

Get the current user's profile and loyalty status.

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
    "tier": {
      "name": "Gold",
      "discountPercentage": 5.0,
      "cashbackPercentage": 5.0,
      "minSpendRequirement": 1000.00,
      "displayOrder": 2,
      "color": "#FFD700"
    },
    "points": 120,
    "spendToNextTier": 500.00,
    "spendToMaintainTier": 200.00,
    "currentMonthSpending": 750.00
  }
}
```

---

### `PUT /api/v1/users/me` 👤 USER+

Update own profile (name and email).

**Request Body**
```json
{
  "name": "Alijon Karimov",
  "email": "alijon@example.com"
}
```

**Response `200 OK`** — Updated user profile.

---

### `GET /api/v1/users` 🔑 ADMIN

List all users (admin panel).

**Query Parameters:** `page`, `size`, `q` (search by name/phone).

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

**Response `200 OK`** — Updated `UserDto`.

---

### `PATCH /api/v1/users/{userId}/status` 🔑 ADMIN

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

Create a new product with variants and SKUs.

**Request Body**
```json
{
  "name": "Classic Hoodie",
  "categoryId": 2,
  "variants": [
    {
      "colorId": 3,
      "skus": [
        { "sizeLabel": "S", "price": 140.00, "stockQuantity": 20 },
        { "sizeLabel": "M", "price": 150.00, "stockQuantity": 30 },
        { "sizeLabel": "L", "price": 160.00, "stockQuantity": 25 }
      ]
    }
  ]
}
```

**Response `201 Created`**
```json
{
  "productBaseId": 5,
  "productCode": "RAD-0005",
  "variants": [
    {
      "variantId": 10,
      "slug": "classic-hoodie-black",
      "colorKey": "BLACK",
      "skus": [ /* SkuDto[] */ ]
    }
  ]
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

Delete a category.

**Response `204 No Content`**

---

## 12. Loyalty Tiers (`/api/v1/admin/loyalty-tiers`)

---

### `GET /api/v1/admin/loyalty-tiers` 🔑 ADMIN

List all loyalty tiers (ordered by `displayOrder`).

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

### `POST /api/v1/admin/loyalty-tiers` 🔑 ADMIN

Create a loyalty tier.

**Request Body**
```json
{
  "name": "Platinum",
  "discountPercentage": 10.0,
  "cashbackPercentage": 10.0,
  "minSpendRequirement": 3000.00,
  "displayOrder": 3,
  "color": "#E5E4E2"
}
```

**Response `201 Created`** — Created `LoyaltyTierDto`.

---

### `PUT /api/v1/admin/loyalty-tiers/{tierId}` 🔑 ADMIN

Update a loyalty tier.

**Request Body** — Same shape as POST.

**Response `200 OK`** — Updated `LoyaltyTierDto`.

---

### `DELETE /api/v1/admin/loyalty-tiers/{tierId}` 🔑 ADMIN

Delete a loyalty tier.

**Response `204 No Content`**

---

## 13. Colors (`/api/v1/admin/colors`)

---

### `GET /api/v1/admin/colors` 🔧 MANAGER+

List all colors.

**Response `200 OK`**
```json
[
  { "id": 1, "colorKey": "BLACK", "displayName": "Black", "hexCode": "#000000" },
  { "id": 2, "colorKey": "WHITE", "displayName": "White", "hexCode": "#FFFFFF" }
]
```

---

### `POST /api/v1/admin/colors` 🔧 MANAGER+

Create a color.

**Request Body**
```json
{
  "colorKey": "NAVY",
  "displayName": "Navy Blue",
  "hexCode": "#001F5B"
}
```

**Response `201 Created`** — Created color.

---

### `PATCH /api/v1/admin/colors/{colorId}` 🔧 MANAGER+

Update color display name or hex code.

---

## 14. Search (`/api/v1/search`)

---

### `POST /api/v1/search/reindex` 🔑 ADMIN

Rebuild the Elasticsearch index from scratch. Run after bulk data imports.

**Response `200 OK`**
```json
{
  "indexed": 1250,
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
| `403` | Insufficient role |
| `404` | Resource not found |
| `409` | Conflict (e.g., duplicate phone) |
| `500` | Internal server error |

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

| Feature | Endpoint |
|---|---|
| Phone login (step 1) | `POST /auth/otp/send` |
| Phone login (step 2) | `POST /auth/otp/verify` |
| Token refresh | `POST /auth/refresh` |
| My profile + loyalty | `GET /users/me` |
| Product list / search | `GET /listings` |
| Product detail | `GET /listings/{slug}` |
| Homepage | `GET /home` |
| Category tree | `GET /categories` |
| Cart | `GET /cart` |
| Add to cart | `POST /cart/items` |
| Update cart item | `PUT /cart/items/{skuId}` |
| Remove from cart | `DELETE /cart/items/{skuId}` |
| Checkout | `POST /orders/checkout` |
| My orders | `GET /orders` |
| Order detail | `GET /orders/{orderId}` |
| Initiate payment | `POST /payments/initiate` |
| Payment status poll | `GET /payments/{id}/status` |
| Upload product images | `POST /listings/{id}/images` |
| Update listing content | `PUT /listings/{id}` |
| Create product | `POST /admin/products` |
| Update price (ADMIN) | `PUT /admin/skus/{id}/price` |
| Update stock (ADMIN) | `PUT /admin/skus/{id}/stock` |
