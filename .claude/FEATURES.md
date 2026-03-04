# Radolfa — Feature Backlog

> Compared against Wildberries, Noon, Amazon. Ranked by business impact for a starter fashion e-commerce.
> Last updated: 2026-03-05

## Phase 1 — Makes it a real store (Priority: now)

| # | Feature | Why it matters | Current state |
|---|---|---|---|
| 1 | **Checkout — place order from cart** | Without this you have a catalog, not a store | Cart is complete, no `/checkout` endpoint |
| 2 | **Delivery address book** | Prerequisite for #1 — where to ship | No `Address` model at all |
| 3 | **Advanced catalog filtering** | Price range, category, color, size — all missing from `GET /listings` | Only `inStock` + 4 sort options exist |
| 4 | **Order cancellation** | Every platform has it; users expect it | `OrderController` is read-only |
| 5 | **Wishlist (backend)** | Frontend stub already exists in the repo | No backend model, port, or migration |

## Phase 2 — Retention & trust

| # | Feature | Why it matters | Current state |
|---|---|---|---|
| 6 | **Product reviews & ratings** | Single biggest trust/conversion signal on Amazon/WB | Zero reviews infrastructure |
| 7 | **Promo / discount codes** | Essential for marketing campaigns and customer acquisition | No coupon model |
| 8 | **Return / refund request** | Without it, returns are handled via phone — kills trust | No `Return` model |
| 9 | **Loyalty points redemption at checkout** | ERP already syncs points balances, but users can't spend them | Sync exists; no redemption logic |
| 10 | **Email / SMS notifications** | Order confirmed, shipped, delivered — users expect this | No notification service |
| 11 | **Order status timeline** | Full history of status changes vs just current status | Only current `OrderStatus` enum stored |
| 12 | **Order history filtering & pagination** | Current endpoint returns all orders as a flat list | No pagination, no filter by status/date |

## Phase 3 — Growth & engagement

| # | Feature | Why it matters | Current state |
|---|---|---|---|
| 13 | **Related / similar products** | "You may also like" — increases basket size | No recommendation logic |
| 14 | **Recently viewed products** | Wildberries and Noon both track this; drives return visits | No browsing history tracking |
| 15 | **Back-in-stock alerts** | Users subscribe to be notified when a sold-out item restocks | No alert/subscription model |
| 16 | **Guest checkout** | Forcing account creation before buying loses customers | OTP required for all purchases |
| 17 | **Product Q&A** | Pre-purchase questions answered by staff; reduces support load | No `Question` model |
| 18 | **Flash sales / timed promotions** | Time-limited pricing drives urgency (Noon/WB use heavily) | No `Promotion` model; prices only from ERP |
| 19 | **Invoice / receipt PDF** | Business customers need this; legally expected in Tajikistan | No PDF generation |
| 20 | **Product bundles / complete the look** | Fashion-specific; "buy the top + trousers together" — common on WB | No bundle model |

## Implementation Notes

- **Checkout (#1)** depends on **Address Book (#2)** — build 2 before 1.
- **Loyalty Redemption (#9)** is backend-only; ERP already syncs point balances via `SyncLoyaltyPointsService`.
- **Wishlist (#5)** has a frontend stub at `frontend/src/shared/lib/wishlist.ts` — backend is the blocker.
- **Filtering (#3)** requires changes to `ListingController`, `GetListingUseCase`, `LoadListingPort`, and the JPQL query in `ListingReadAdapter`.
- **Reviews (#6)** must NOT allow ERP to overwrite — purely Radolfa-owned data.
- **Flash sales (#18)** — pricing must still flow from ERP; promotions are percentage/flat discounts applied on top.
