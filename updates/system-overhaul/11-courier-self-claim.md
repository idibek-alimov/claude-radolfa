# Courier Self-Claim (Pull Delivery Model) — Multiphase Plan

> **Deliverable:** Save this document as
> `updates/system-overhaul/11-courier-self-claim.md` (mirrors the structure of
> `10-pick-order-improvements.md`).
>
> **Rule:** After completing any phase, mark it `✅ Complete` in the status table below.

## Phase Completion Status

| Phase | Description | Status |
|---|---|---|
| 1 | Backend: domain — add `CLAIMED` `OrderStatus` + `claimedAt` timestamp (enum, `Order` record/builder, `OrderEntity`, `OrderMapper`, edit-original `V3__commerce.sql`) | ✅ Complete |
| 2 | Backend: available-pool query + concurrency-safe `ClaimOrderUseCase`/service + `POST /courier/orders/{id}/claim` + `GET /courier/orders/available` | ⬜ Not started |
| 3 | Backend: rewire pipeline — `CLAIMED → OUT_FOR_DELIVERY` collect, delivery-code at `CLAIMED`, drop admin HOME ship-assignment from transition matrix, admin unclaim (`CLAIMED → PICKED`) | ⬜ Not started |
| 4 | Frontend (courier): **Available** tab + **Claim** action; **To Collect** now reads `CLAIMED`; new hooks | ✅ Complete |
| 5 | Frontend (admin): remove HOME ship/assign-courier modal; read-only monitoring + **Unclaim** rescue action | ⬜ Not started |
| 6 | Frontend (customer + bridge): add `CLAIMED` to TS `OrderStatus`; i18n (internal "Claimed" / customer "Being prepared"); customer timeline mapping | ⬜ Not started |

---

## Context

Today every paid order forces the **admin to manually assign a courier**. After the
warehouse completes the pick (`PICKED`), the order sits until an admin opens the
order detail, clicks **Mark as Shipped**, picks a courier from a dropdown, and
enters tracking — `ShipOrderModal.tsx` → `PATCH /api/v1/orders/{id}/status` with
`courierId`, which `UpdateOrderStatusService` writes onto the order during the
`PICKED → SHIPPED` transition (`UpdateOrderStatusService.java:60-90`). Only after
that does the order appear in a courier's list, because the courier feed is filtered
strictly by assigned `courierId` (`OrderRepositoryAdapter.findByCourierIdAndStatusIn`).

This is a hard bottleneck: tolerable at a handful of orders/day, a full-time clicking
job at scale. It is also the reported symptom — *"when I paid, the admin had no clear
action and nothing showed for the delivery account."* The infrastructure (courier
dashboard, `/collect`, delivery-code confirmation) is fully built; the missing link is
the manual admin assignment in the middle.

**Intended outcome — a pull model (Option B):** the warehouse marks an order
`PICKED` (already an explicit button after `10-pick-order-improvements`), which makes
it appear in a shared **Available** pool for couriers. Any courier **claims** an
order — it becomes `CLAIMED` and is bound to that courier — then physically
**collects** it (`OUT_FOR_DELIVERY`) and **delivers** it against the existing SMS
code. The **admin is removed from the per-order loop**; they monitor only, with a
single **unclaim** rescue action for an order a courier claims and then abandons.

**New home-delivery flow:**

```
PAID → PICKED ──(courier claims)──▶ CLAIMED ──(courier collects)──▶ OUT_FOR_DELIVERY ──(SMS code)──▶ DELIVERED
        ▲ available pool             ▲ bound to courier,
        (courier_id IS NULL)           delivery code sent
```

`CLAIMED` is a **new, internal** status: it means *"a courier owns this but has not
yet physically collected it."* It is the pull-model analogue of what `SHIPPED` meant
under the push model, and is **never surfaced to the customer** — to the buyer it
reads as "being prepared," identical to `PICKED`.

**Owner decisions (locked):**

1. **Failed delivery (`DELIVERY_ATTEMPTED`) stays with the same courier** — the order
   keeps its `courierId`; the courier retries on a later run (today's behavior). The
   admin **unclaim** action is the escape hatch if the courier never completes it.
2. **Pull model is HOME-delivery only.** Pickpoint orders keep the existing
   admin-driven pipeline (`PICKED → SHIPPED → READY_FOR_PICKUP → DELIVERED`).
   `ShipOrderModal` and the `PICKED → SHIPPED` admin transition remain **for pickpoint
   orders only**.
3. **Admin unclaim is in scope now** — `CLAIMED → PICKED`, clearing `courierId`,
   returning the order to the pool.

**Out of scope:**

- Zone-/capacity-based **auto-assignment** (Option C) — explicitly deferred to scale.
- Touching the pickpoint pipeline beyond gating it out of the claim pool.
- Stuck-claim **timeout/SLA auto-flagging** (manual unclaim only for now).
- Changing the delivery-code confirmation logic itself.
- Backfilling historical orders (dev wipes & re-seeds).

---

## Constraints & Principles

- **Hexagonal backend, FSD frontend.** Conventions in root, `backend/CLAUDE.md`,
  `frontend/CLAUDE.md` apply.
- **No client-side filtering of paginated lists.** Both the Available pool and the
  courier "my orders" feed are filtered **server-side** by status + assignment, never
  by trimming `content[]`.
- **Status mutation flows through services, not adapters.** Claim/unclaim are new
  application use cases; `@Transactional` on the service, constructor injection only.
- **Claiming must be concurrency-safe.** Two couriers tapping *Claim* on the same
  order must not both win. Use a **conditional update** in the adapter
  (`UPDATE ... SET courier_id = ? WHERE id = ? AND courier_id IS NULL AND status =
  'PICKED'`) and treat *zero rows affected* as "already claimed" → `409`. Do **not**
  rely solely on a read-then-write check inside the transaction.
- **Dev DB policy — edit original migrations, no `ALTER TABLE`.** The new
  `claimed_at` column and any supporting index go into the original
  `V3__commerce.sql` (which creates `orders`), not a new migration version.
- **Domain stays pure.** `Order` is a `record` with a `Builder`; add `claimedAt`
  there and in `toBuilder()`. No Spring/JPA leaks into domain.
- **Tests use hand-written fakes, no Mockito.** New `ClaimOrderServiceTest`,
  `UnclaimOrderServiceTest`; update `MarkOutForDeliveryServiceTest` (now requires
  `CLAIMED`) and `UpdateOrderStatusServiceTest` (HOME `PICKED → SHIPPED` removed).
- **`CLAIMED` is internal-only to customers.** Customer surfaces map it to the same
  "being prepared" wording as `PICKED`; the customer timeline does **not** show it as
  a distinct step. Internal (admin/courier) surfaces label it "Claimed".
- All new user-visible strings use the existing i18n namespaces with full **en / ru /
  tj** translations (`courier`, `manage.orders.status`, `profile.status`).

---

## Phase 1 — Backend: `CLAIMED` status + `claimedAt` timestamp

**Goal:** The domain and persistence layers understand a new `CLAIMED` state and
record when a claim happened. No behavior wired yet.

### 1a — Enum value

**`backend/src/main/java/tj/radolfa/domain/model/OrderStatus.java`** — add `CLAIMED`
immediately after `PICKED`:

```
PENDING, PAID, PICKED, CLAIMED, SHIPPED, OUT_FOR_DELIVERY, ...
```

### 1b — Domain `Order` record

**`backend/src/main/java/tj/radolfa/domain/model/Order.java`** — add `Instant
claimedAt` to the record header, the `Builder` (field + fluent setter), `toBuilder()`,
and the `build()` constructor call — mirroring the existing `shippedAt` /
`outForDeliveryAt` timestamps (lines 26-43, builder 87/123).

### 1c — JPA entity + mapper

- **`OrderEntity`** — add `private Instant claimedAt;` with `@Column(name =
  "claimed_at")`, mirroring `shippedAt`.
- **`OrderMapper`** — `claimedAt` is auto-mapped by name; no explicit `@Mapping`
  needed (confirm both directions at implementation time).

### 1d — Migration (edit-original)

**`backend/src/main/resources/db/migration/V3__commerce.sql`** — inside the existing
`CREATE TABLE orders` block, add `claimed_at TIMESTAMP` next to `shipped_at`. Add a
**partial index** to make the Available-pool query cheap:

```sql
CREATE INDEX idx_orders_available_pool ON orders (status) WHERE courier_id IS NULL;
```

No new migration version; no `ALTER TABLE`.

### 1e — Verification

1. `./mvnw clean compile -pl backend` — green.
2. Fresh dev DB boots; `\d orders` shows `claimed_at` and the partial index.
3. `./mvnw test -pl backend` — existing suite still green (no behavior change yet).

---

## Phase 2 — Backend: Available pool + courier self-claim

**Goal:** Couriers can list unassigned `PICKED` home orders and atomically claim one.

### 2a — Available-pool query (port + adapter)

- **`LoadCourierOrdersPort`** — add
  `PageResult<Order> loadAvailablePoolPaged(int page, int size)`.
- **`OrderRepositoryAdapter`** + Spring Data repo — add
  `findByStatusAndCourierIdIsNullAndDeliveryType(OrderStatus.PICKED, DeliveryType.HOME,
  pageable)` sorted by `createdAt` ascending (oldest first — fairness). HOME-only so
  pickpoint orders never enter the pool.

### 2b — Concurrency-safe claim (port + adapter)

- **New `ClaimOrderPort`** with `boolean claimIfAvailable(Long orderId, Long
  courierId)` → implemented in the adapter as a **conditional update**:

  ```sql
  UPDATE orders
     SET courier_id = :courierId, status = 'CLAIMED', claimed_at = now()
   WHERE id = :orderId AND courier_id IS NULL AND status = 'PICKED'
     AND delivery_type = 'HOME'
  ```

  Returns `true` iff exactly one row was updated. This is the single source of truth
  for "did I win the claim" — no read-then-write race.

### 2c — `ClaimOrderUseCase` + `ClaimOrderService`

- **`application/ports/in/order/ClaimOrderUseCase.java`** (new):
  `record Command(Long orderId, Long courierId) {}` + `void execute(Command cmd);`
- **`application/services/ClaimOrderService.java`** (new), `@Service @Transactional`:
  1. `claimOrderPort.claimIfAvailable(orderId, courierId)`; if `false` →
     `OrderAlreadyClaimedException` (new domain exception → **409** in
     `GlobalExceptionHandler`).
  2. Reload the now-`CLAIMED` order via `LoadOrderPort`.
  3. `generateDeliveryCodeUseCase.execute(orderId)` — code generated **at claim**
     (see Phase 3c for the guard change).
  4. `orderNotificationService.notify(updated)`.

### 2d — Courier endpoints

**`infrastructure/web/CourierController.java`**:

- `GET /api/v1/courier/orders/available` (`hasRole('COURIER')`, paginated) → maps the
  pool to the existing `CourierOrderDto` (it already carries customer/address/weight).
- `POST /api/v1/courier/orders/{orderId}/claim` (`hasRole('COURIER')`) →
  `claimOrderUseCase.execute(new Command(orderId, principal.userId()))` → `204`, or
  `409` if already claimed.

Register both paths in `SecurityConfig`.

### 2e — Tests

- **`ClaimOrderServiceTest`** (fakes): available HOME `PICKED` → `CLAIMED` + courier
  bound + delivery code generated; second claim on same order → throws (no double
  bind); non-`PICKED` / pickpoint / already-assigned → not claimable.
- Pool query: assert pickpoint and already-assigned orders are excluded (repo slice
  test if that pattern exists, else document manual check as in Phase 2 of
  `10-pick-order-improvements`).

### 2f — Verification

1. `./mvnw test -pl backend` — green.
2. Seed a HOME `PICKED` order with `courier_id IS NULL`.
3. `GET /courier/orders/available` → returns it; pickpoint `PICKED` orders absent.
4. `POST /courier/orders/{id}/claim` → `204`; order is `CLAIMED`, `courier_id` set,
   `claimed_at` set, a delivery code row exists, SMS notification fired.
5. Second `claim` (different courier) → `409`; first courier still owns it.

---

## Phase 3 — Backend: Rewire the delivery pipeline

**Goal:** Collect transitions from `CLAIMED`; the admin HOME ship/assign path is
removed; admin can unclaim; pickpoint is untouched.

### 3a — Collect from `CLAIMED`

**`application/services/MarkOutForDeliveryService.java`** (lines 36-44) — change the
required current status from `SHIPPED` to `CLAIMED`. The existing ownership guard
(`order.courierId().equals(courierId)`) is unchanged and now enforces that only the
claiming courier can collect.

### 3b — Transition matrix

**`application/services/UpdateOrderStatusService.java`** `validateTransition`
(lines 110-120):

- `PICKED` (HOME) → **remove** `SHIPPED` (couriers claim instead). `PICKED`
  (pickpoint) → keep `SHIPPED` / `READY_FOR_PICKUP`.
- Add admin **unclaim**: `CLAIMED → PICKED` (HOME). On this transition, clear
  `courierId`/`claimedAt` (extend the field-reset logic at lines 60-79, analogous to
  how `toShipped` gates field assignment — add a `toUnclaim` branch that nulls
  `courierId`).
- `DELIVERY_ATTEMPTED` stays owned by the courier (locked decision); leave its
  retry handling as-is (no `→ SHIPPED` reschedule needed in the pull model — the
  courier still holds the parcel).
- Drop `validateCourierFields`' HOME branch (no admin courier assignment for HOME);
  keep any pickpoint relevance.

### 3c — Delivery-code trigger moves to claim

- **`UpdateOrderStatusService`** (lines 82-84) — the auto-generate-on-`SHIPPED`
  trigger now only fires for **pickpoint** `READY_FOR_PICKUP` (HOME no longer passes
  through `SHIPPED`). Generation for HOME happens in `ClaimOrderService` (Phase 2c).
- **`GenerateDeliveryCodeService`** (guard at lines 52-55) — add `CLAIMED` to the
  accepted statuses alongside `SHIPPED` / `READY_FOR_PICKUP`.

### 3d — Admin unclaim endpoint

Reuse the existing `PATCH /api/v1/orders/{id}/status` admin path
(`OrderController:161`) — admin sends `{ status: "PICKED" }` on a `CLAIMED` order; the
new `CLAIMED → PICKED` transition (3b) handles the field reset. No new endpoint
required. (Confirm `AdminOrderController` exposure if the frontend prefers a dedicated
verb; default is reuse.)

### 3e — Courier "my orders" default statuses

**`CourierController.DEFAULT_STATUSES`** (line 61) — replace `SHIPPED` with
`CLAIMED`: `[CLAIMED, OUT_FOR_DELIVERY, DELIVERY_ATTEMPTED]`. (Pickpoint `SHIPPED`
orders are not courier-claimable, so they correctly drop out of the courier feed.)

### 3f — WebSocket push

**`UpdateOrderStatusService`** lines 89-93 / `ClaimOrderService` — on successful
claim, optionally publish `publishOrderAssignedToCourier(courierId, orderId)` so the
courier's other devices refresh. Pool changes can reuse the existing
`["courier-orders"]` invalidation on the frontend.

### 3g — Tests

- **`MarkOutForDeliveryServiceTest`** — require `CLAIMED` (was `SHIPPED`); non-owner
  courier rejected.
- **`UpdateOrderStatusServiceTest`** — HOME `PICKED → SHIPPED` now invalid; HOME
  `CLAIMED → PICKED` valid and clears `courierId`; pickpoint paths unchanged.
- **`UnclaimOrderServiceTest`** (or extension of the update-status test) — `CLAIMED →
  PICKED` returns the order to the pool with `courier_id` null.

### 3h — Verification

1. `./mvnw test -pl backend` — green.
2. Claim → `/collect` → `OUT_FOR_DELIVERY`; confirm with code → `DELIVERED`.
3. Admin `PATCH .../status {PICKED}` on a `CLAIMED` order → back in the pool,
   `courier_id` null; reappears in `GET /courier/orders/available`.
4. Pickpoint order still ships via admin `ShipOrderModal` path unchanged.

---

## Phase 4 — Frontend (courier): Available tab + Claim

**Goal:** Couriers see and claim available orders; the collect flow now starts from
`CLAIMED`.

### 4a — Hooks (`frontend/src/features/courier/api.ts`)

- `useAvailableOrders(page, size)` → `GET /api/v1/courier/orders/available`
  (query key `["courier-available", page]`).
- `useClaimOrder()` → `POST /api/v1/courier/orders/{orderId}/claim`; `onSuccess`
  invalidate `["courier-available"]` + `["courier-orders"]`; `onError` toast
  (handle `409` → "already claimed by another courier").

### 4b — Dashboard tab (`CourierDashboardPage.tsx`)

- Add a leading **Available** tab driven by `useAvailableOrders`.
- Change `COLLECT_STATUSES` from `["SHIPPED"]` to `["CLAIMED"]` (lines 27-29) — the
  "To Collect" tab now lists the courier's own claimed-not-collected orders.
- Badge counts: add the available count.

### 4c — Card actions (`CourierOrderCard.tsx`)

- Available-pool card → primary action **"Claim this order"** → `useClaimOrder`.
- `CLAIMED` card → existing **"I've Collected This Order"** (`useMarkCollected`,
  `/collect`) — switch its trigger status from `SHIPPED` to `CLAIMED` (lines 73-101).
- `OUT_FOR_DELIVERY` / `DELIVERY_ATTEMPTED` actions unchanged.

### 4d — i18n (`courier` block, en/ru/tj)

Add `available`, `claim`, `claiming`, `claimSuccess`, `claimConflict` ("Already
claimed by another courier").

### 4e — Verification

1. `npm run lint --prefix frontend` + `npx tsc --noEmit` — clean.
2. Two courier sessions: order shows in **Available** for both; one claims → it leaves
   the other's Available list and appears in the claimer's **To Collect**; the loser
   sees the conflict toast.
3. Collect → In Transit; confirm with code → gone. en/ru/tj render.

---

## Phase 5 — Frontend (admin): remove assignment, add monitoring + unclaim

**Goal:** Admin no longer assigns couriers for HOME orders; they monitor and can
rescue a stuck claim.

### 5a — `AdminOrderDetailView.tsx`

- `showShipButton` (line 132) — restrict to **pickpoint** orders only (HOME no longer
  uses the ship/assign modal). HOME `PICKED` shows a read-only **"Awaiting courier
  claim"** banner.
- `CLAIMED` (HOME) — show the **claiming courier** (name/id) read-only + an
  **Unclaim / return to pool** action → `useUpdateOrderStatus.mutate({ orderId,
  status: "PICKED" })` (reuses the existing hook; backend resets `courierId`).
- `nextStatusFor` / fulfillment timeline (lines 48-56) — insert `CLAIMED` between
  `PICKED` and `OUT_FOR_DELIVERY` for HOME.

### 5b — `ShipOrderModal.tsx`

- Keep the component but render/trigger it only for **pickpoint** orders. (Do not
  delete — pickpoint still relies on it.)

### 5c — i18n (`manage` block, en/ru/tj)

Add `awaitingClaim`, `claimedBy`, `unclaim`, `unclaimConfirm`.

### 5d — Verification

1. Lint + `tsc` clean.
2. HOME `PICKED` order detail shows "Awaiting courier claim" (no courier dropdown).
3. After a courier claims → admin sees `CLAIMED` + courier name + **Unclaim**;
   clicking it returns the order to the pool (verified in courier Available tab).
4. Pickpoint order detail still shows the ship modal and works end-to-end.

---

## Phase 6 — Frontend (customer + bridge): status mapping

**Goal:** `CLAIMED` is internal; customers see "being prepared". Bridge type updated.

### 6a — Bridge type

**`frontend/src/entities/order/model/types.ts`** — add `"CLAIMED"` to the
`OrderStatus` union (keep the "Synced from backend" header). Run `/bridge` to confirm
parity with the Java enum.

### 6b — Status labels (`OrderStatusBadge` + i18n)

- **`manage.orders.status`** (en/ru/tj) — add `CLAIMED`: en "Claimed" / ru "Принят
  курьером" / tj equivalent. Pick a `STATUS_STYLES` entry in
  `entities/order/ui/OrderStatusBadge.tsx` (reuse the SHIPPED style).
- **`profile.status`** (en/ru/tj) — add `CLAIMED` mapped to the **same** wording as
  `PICKED` ("Being prepared" / "Готовится" / …) so the customer never distinguishes
  it.

### 6c — Customer timeline (`CustomerOrderDetailPage.tsx`)

- `HOME_STEPS` (lines 29-47) currently `["PENDING","PAID","SHIPPED","DELIVERED"]`.
  Since HOME orders no longer pass through `SHIPPED`, update the home timeline to
  `["PENDING","PAID","OUT_FOR_DELIVERY","DELIVERED"]` with the existing
  `orderShipped`/"Out for delivery" step label, and make `getStepIndex` map both
  `PICKED` and `CLAIMED` to the "Paid / being prepared" step (not a distinct node).
  Pickpoint steps unchanged.

### 6d — Verification

1. `/bridge` clean; `tsc` + lint clean.
2. As a customer, a `CLAIMED` order reads "Being prepared" in history and on the
   detail timeline (same as `PICKED`) — no "Claimed" leak.
3. As admin/courier, the same order reads "Claimed".
4. en/ru/tj render with no missing-key warnings.

---

## Post-Implementation Checklist

- [ ] `CLAIMED` added to `OrderStatus` (domain) and the frontend `OrderStatus` union;
      `/bridge` parity confirmed.
- [ ] `claimed_at` + `idx_orders_available_pool` added to original `V3__commerce.sql`
      (no `ALTER TABLE`, no new migration version); `Order` record/builder, JPA
      entity, mapper updated.
- [ ] Available pool query is HOME-only, `courier_id IS NULL`, status `PICKED`,
      oldest-first; pickpoint excluded.
- [ ] Claim is **concurrency-safe** via conditional `UPDATE` (zero-rows → `409`);
      `ClaimOrderServiceTest` proves no double-claim.
- [ ] Delivery code generated at `CLAIMED` (HOME) / `READY_FOR_PICKUP` (pickpoint);
      `GenerateDeliveryCodeService` guard accepts `CLAIMED`.
- [ ] `MarkOutForDeliveryService` requires `CLAIMED`; ownership guard intact.
- [ ] Admin HOME `PICKED → SHIPPED` assignment removed; `CLAIMED → PICKED` unclaim
      added and clears `courierId`/`claimedAt`; pickpoint pipeline unchanged.
- [ ] `CourierController.DEFAULT_STATUSES` = `[CLAIMED, OUT_FOR_DELIVERY,
      DELIVERY_ATTEMPTED]`; `/available` + `/claim` endpoints added and secured.
- [ ] Courier UI: **Available** tab + **Claim**; **To Collect** reads `CLAIMED`;
      `409` conflict handled with a toast — no client-side list filtering.
- [ ] Admin UI: HOME shows "Awaiting claim" / claiming courier + **Unclaim**; ship
      modal restricted to pickpoint.
- [ ] Customer never sees `CLAIMED` as distinct — maps to "being prepared"; home
      timeline updated to `PENDING→PAID→OUT_FOR_DELIVERY→DELIVERED`.
- [ ] New tests are fake-based (no Mockito): `ClaimOrderServiceTest`, unclaim
      coverage, updated `MarkOutForDeliveryServiceTest` + `UpdateOrderStatusServiceTest`.
- [ ] `./mvnw test -pl backend` green; `npm run lint` + `npx tsc --noEmit` clean;
      each phase committed `Phase N | Courier Self-Claim: <change>`.
```
