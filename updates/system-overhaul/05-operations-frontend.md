# Operations Frontend — Multiphase Plan

> **Rule:** After completing any phase, mark it `✅ Complete` in the status table below.

## Phase Completion Status

| Phase | Description | Status |
|---|---|---|
| 1 | Admin Order Tabs + Bypass Fix | ✅ Complete |
| 2 | Admin Order Detail Enhancements | ✅ Complete |
| 3 | Pickpoint Staff Dashboard Improvements | ✅ Complete |
| 4 | Courier Dashboard Server-Side Wiring | ✅ Complete |

---

## Context

Three operational surfaces — the admin order list, pickpoint staff dashboard, and courier dashboard — each have a distinct failure mode:

**Admin order list (P1, P2):** The order management table is a flat list filtered by a single status dropdown. Admins must mentally categorise what needs attention today versus what is in motion versus what is closed. Worse, the `nextStatusFor()` function contains a PAID → READY_FOR_PICKUP shortcut that lets an admin bypass the physical courier-to-pickpoint delivery step entirely, marking a package as "ready for collection" before it has physically arrived.

**Admin order detail (P8, P9):** When a customer initiates a return, there is no customer return section visible on the order detail. An admin reviewing an order has no idea a return request exists without navigating to a separate queue. Similarly, for pickpoint orders, the admin can see the pickpoint name but has no record of when the package arrived or who on staff confirmed it.

**Pickpoint staff dashboard (P10, P11, P12):** The incoming-packages card shows only the order number and customer name — staff cannot see how many items are in the package or what they are. Confirming multiple arrivals requires clicking one-by-one. The 5-tab bar is cramped in a `grid-cols-5` layout that overflows on small screens.

**Courier dashboard (P13 frontend, P14 frontend):** Three client-side `.filter()` calls split the fetched order array into tabs by status. This violates the server-side-only filtering rule: couriers who have many past orders will see silently incomplete lists on each tab.

**Problems addressed:**
- **P1** — Admin order list has no tab structure separating actionable orders from in-progress and completed.
- **P2** — `nextStatusFor()` allows `PAID → READY_FOR_PICKUP` bypass, skipping physical delivery.
- **P8** — No customer return information visible on the admin order detail.
- **P9** — No pickpoint arrival timestamp or confirming staff member visible on admin order detail.
- **P10** — `IncomingPackageCard` shows no item details.
- **P11** — No multi-select bulk arrival confirmation.
- **P12** — Pickpoint dashboard tab bar is cramped and overflows on mobile.
- **P13 frontend** — Courier dashboard tabs use client-side `.filter()` on paginated results.
- **P14 frontend** — Courier orders have no pagination controls at all.

---

## Current Baseline

- **Admin orders page shell:** `frontend/src/app/(admin)/manage/orders/page.tsx` (18 lines) — renders `<OrderKpiRibbon />` + `<OrderManagementTable />`. No tabs.
- **`OrderManagementTable.tsx`:** `frontend/src/features/order-management/ui/OrderManagementTable.tsx` (209 lines). Status filter is a single Shadcn `<Select>` dropdown with `STATUS_OPTIONS`. Missing from dropdown: `OUT_FOR_DELIVERY`, `DELIVERY_ATTEMPTED`, `REFUNDED`. Filter state is `useState` (no URL sync). Uses `useAdminOrders({ page, search, status, sortBy, sortDir, size })` at `entities/order/api/index.ts:14` — accepts a single `status: OrderStatus | ""`.
- **`AdminOrderDetailView.tsx`:** `frontend/src/features/order-management/ui/AdminOrderDetailView.tsx` (408 lines). `nextStatusFor()` at lines 46–54 contains the bypass: `case "PAID": return isPickpoint ? "READY_FOR_PICKUP" : null;`. `SectionCard` component defined locally at lines 58–67 — not exported. No customer return section. No pickpoint arrival metadata display.
- **`AdminOrderDetail` type** at `entities/order/model/types.ts:48–77`: has `pickpointId`, `pickpointName`, `pickpointAddress` — **MISSING** `pickpointArrivedAt`, `pickpointConfirmedByUserName`. Has no `customerReturn` or `customerReturns` field.
- **`IncomingPackageCard.tsx`:** `frontend/src/features/pickpoint/ui/IncomingPackageCard.tsx` (49 lines). Props: `{ order: PickpointOrder }`. Shows order ID, customer name, confirm arrival button. `PickpointOrder` at `entities/user/model/types.ts:56–66` — **MISSING** `totalItemCount`, `totalWeightKg`, `items[]`. `CourierOrder` already has `totalItemCount` + `totalWeightKg` (pattern proven).
- **`PickpointDashboardPage.tsx`:** `frontend/src/features/pickpoint/ui/PickpointDashboardPage.tsx` (188 lines). `TabsList className="w-full grid grid-cols-5"`. Five tab values: `incoming`, `awaiting`, `returns`, `customer-returns`, `history`. Container `max-w-lg mx-auto px-4 py-6` (mobile-first). Tabs use internal state — no URL sync.
- **`useConfirmArrival`** at `features/pickpoint/api.ts:60` — per-ID mutation `POST /api/v1/pickpoint/orders/{orderId}/confirm-arrival`. No bulk endpoint.
- **`CourierDashboardPage.tsx`:** `frontend/src/features/courier/ui/CourierDashboardPage.tsx` (115 lines). `useCourierOrders()` at `features/courier/api.ts:15` — `GET /api/v1/courier/orders → CourierOrder[]` (unpaginated). Lines 53–55: three `.filter()` calls split the array by status — correctness bug.
- **i18n:** `next-intl`. Admin namespace `manage.orders` is rich and fully in use. Pickpoint and courier UIs have **zero i18n** — all strings are hardcoded English.
- **Multi-select pattern reference:** `features/discount-management/ui/DiscountTable.tsx` lines 210 + 568 (Set-based selection + indeterminate Checkbox) + lines 494–523 (inline bulk action bar). No `fixed bottom` sticky bar exists anywhere — fresh pattern needed for mobile pickpoint UX.
- **Horizontal scroll pattern reference:** `entities/review/ui/ReviewVariantFilterStrip.tsx:21` — `flex gap-2 overflow-x-auto pb-1 scrollbar-hide` + `shrink-0 whitespace-nowrap` on each item.
- **Backend admin orders endpoint:** `GET /api/v1/admin/orders?page&size&search&status&sortBy&sortDir`. Currently accepts one `status` value. Needs a `statuses` CSV param for tab-based multi-status filtering (Phase 1 includes this small backend addition).

---

## Constraints & Principles

- **FSD:** `app → features → entities → shared`. Route shells ≤10 lines. No logic in `app/` pages.
- **Server-side filtering (non-negotiable):** No `.filter()`, `.sort()`, `.slice()` on paginated `content[]`. Each tab must send its own `statuses` or `status` param to the backend.
- **`"use client"` only when necessary:** components with hooks or event handlers; structural wrappers stay server components.
- **i18n all user-visible strings:** `useTranslations("namespace")`. No hardcoded English (or other language) in JSX.
- **TanStack Query v5:** `keepPreviousData` on paginated queries. Query key includes all filter params.
- **No Mockito (backend):** Any small backend additions use hand-written in-memory fake adapters for tests.
- **Constructor injection only (backend).** `@Transactional` on application services only.
- **Axios:** default import `import apiClient from "@/shared/api/axios"`.
- **Error handling:** `toast.error(getErrorMessage(err))` from `@/shared/lib`.
- Run `npm run lint --prefix frontend` and `npx tsc --noEmit --prefix frontend` after each phase.

---

## Phase 1 — Admin Order Tabs + Bypass Fix

**Goal:** Replace the status dropdown in the admin order list with three workflow-oriented tabs (Needs Action, In Fulfillment, Completed), sync the active tab to the URL, and remove the `PAID → READY_FOR_PICKUP` status bypass from the order detail view.

### 1a — Backend: Add `statuses` CSV param to admin orders endpoint

**`backend/src/main/java/tj/radolfa/infrastructure/web/AdminOrderController.java`** (Modify)

Add an optional `statuses` query param alongside the existing `status` param. When `statuses` is provided it takes precedence; `status` remains for backward compatibility:

```java
@GetMapping
public PageResponse<AdminOrderListDto> listOrders(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String search,
    @RequestParam(required = false) OrderStatus status,
    @RequestParam(required = false) String statuses,  // ← new: "PENDING,PAID,DELIVERY_ATTEMPTED"
    @RequestParam(required = false) String sortBy,
    @RequestParam(required = false) String sortDir
) {
    List<OrderStatus> statusFilter = statuses != null
        ? Arrays.stream(statuses.split(","))
                .map(OrderStatus::valueOf)
                .toList()
        : (status != null ? List.of(status) : List.of());
    return orderService.listOrders(page, size, search, statusFilter, sortBy, sortDir);
}
```

**`backend/src/main/java/tj/radolfa/application/ports/in/GetAdminOrdersUseCase.java`** (Modify)

Extend signature to accept a `List<OrderStatus>` for the status filter (or keep backward-compat overload):

```java
PageResult<Order> execute(int page, int size, String search,
                          List<OrderStatus> statuses, String sortBy, String sortDir);
```

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/repository/OrderJpaRepository.java`** (Modify)

Add an overload that filters by a list of statuses:

```java
@Query("""
    SELECT o FROM OrderEntity o
    WHERE (:statuses IS EMPTY OR o.status IN :statuses)
      AND (:search IS NULL OR LOWER(o.user.firstName) LIKE LOWER(CONCAT('%', :search, '%')))
    ORDER BY o.createdAt DESC
    """)
Page<OrderEntity> findByStatusesAndSearch(
    @Param("statuses") Collection<OrderStatus> statuses,
    @Param("search") String search,
    Pageable pageable
);
```

### 1b — Frontend: Update `useAdminOrders` hook

**`frontend/src/entities/order/api/index.ts`** (Modify)

Extend the params type to accept `statuses?: string` (comma-separated) alongside `status`:

```ts
interface AdminOrdersParams {
  page?: number;
  size?: number;
  search?: string;
  status?: OrderStatus | "";
  statuses?: string;       // ← new: "PENDING,PAID,DELIVERY_ATTEMPTED,RETURN_INITIATED"
  sortBy?: string;
  sortDir?: string;
}

export function useAdminOrders(params: AdminOrdersParams) {
  return useQuery({
    queryKey: ["admin-orders", params.page, params.search, params.status,
                params.statuses, params.sortBy, params.sortDir, params.size],
    queryFn: () => apiClient.get("/api/v1/admin/orders", { params }).then(r => r.data),
    placeholderData: keepPreviousData,
  });
}
```

### 1c — Define tab groups

**`frontend/src/features/order-management/ui/OrderManagementTable.tsx`** (Modify)

Add tab group constants at the top of the file:

```ts
const TAB_GROUPS = {
  "needs-action": {
    label: "tabNeedsAction",
    statuses: ["PENDING", "PAID", "DELIVERY_ATTEMPTED", "RETURN_INITIATED"],
  },
  "in-fulfillment": {
    label: "tabInFulfillment",
    statuses: ["SHIPPED", "OUT_FOR_DELIVERY", "READY_FOR_PICKUP", "RETURNED_TO_WAREHOUSE"],
  },
  "completed": {
    label: "tabCompleted",
    statuses: ["DELIVERED", "CANCELLED", "REFUNDED"],
  },
} as const;

type TabKey = keyof typeof TAB_GROUPS;
```

### 1d — Sync active tab to URL and replace the status `<Select>` with `<Tabs>`

**`frontend/src/features/order-management/ui/OrderManagementTable.tsx`** (Modify)

1. Read `tab` query param from URL using Next.js `useSearchParams`. Default to `"needs-action"`.
2. On tab change call `router.replace(?tab=<value>)` — preserve `search` and `page` params.
3. Remove the `<Select>` status dropdown. The status filter is now driven by `TAB_GROUPS[activeTab].statuses`.
4. Keep the text search input and column sort controls.
5. Pass `statuses` (joined with comma) to `useAdminOrders` instead of `status`:

```ts
const searchParams = useSearchParams();
const router = useRouter();
const activeTab = (searchParams.get("tab") ?? "needs-action") as TabKey;
const [search, setSearch] = useState(searchParams.get("search") ?? "");
const [page, setPage] = useState(1);

const { data, isLoading } = useAdminOrders({
  page,
  size: 20,
  search,
  statuses: TAB_GROUPS[activeTab].statuses.join(","),
  sortBy: "createdAt",
  sortDir: "desc",
});
```

6. Wrap the existing table in Shadcn `<Tabs>` with `value={activeTab}` + `onValueChange`:

```tsx
<Tabs value={activeTab} onValueChange={(v) => {
  setPage(1);
  router.replace(`?tab=${v}`);
}}>
  <TabsList className="mb-4">
    {Object.entries(TAB_GROUPS).map(([key, group]) => (
      <TabsTrigger key={key} value={key}>
        {t(group.label)}
        {data && (
          <TabBadge count={data.totalElements} />
        )}
      </TabsTrigger>
    ))}
  </TabsList>
  {/* Existing table body below — no per-tab <TabsContent> wrapping needed */}
</Tabs>
```

`TabBadge` is an existing component from `PickpointDashboardPage` — if it isn't exported, inline a simple `<span className="ml-1.5 rounded-full bg-muted px-1.5 py-0.5 text-xs">{count}</span>`.

### 1e — Remove `PAID → READY_FOR_PICKUP` bypass in `AdminOrderDetailView`

**`frontend/src/features/order-management/ui/AdminOrderDetailView.tsx`** (Modify)

Lines 46–54 — change `nextStatusFor()`:

```ts
// Before:
case "PAID": return isPickpoint ? "READY_FOR_PICKUP" : null;

// After:
case "PAID": return null;
```

No other changes to `nextStatusFor()`. The Advance button will now be hidden for PAID pickpoint orders. The Ship button (`ShipOrderModal`) already handles the HOME+PAID → SHIPPED transition correctly and is unaffected.

### 1f — Add "awaiting arrival" callout on order detail

**`frontend/src/features/order-management/ui/AdminOrderDetailView.tsx`** (Modify)

After the status badge in the header section, add a callout when the order is `SHIPPED` with `deliveryType === "PICKPOINT"`:

```tsx
{order.status === "SHIPPED" && order.deliveryType === "PICKPOINT" && (
  <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
    {t("awaitingArrivalCallout")}
  </div>
)}
```

### 1g — Add i18n keys

**All three locale files** — under `"manage.orders"` namespace:

```json
// en
"tabNeedsAction": "Needs Action",
"tabInFulfillment": "In Fulfillment",
"tabCompleted": "Completed",
"awaitingArrivalCallout": "Awaiting pickup point staff to confirm package arrival."

// ru
"tabNeedsAction": "Требует действий",
"tabInFulfillment": "В процессе",
"tabCompleted": "Завершено",
"awaitingArrivalCallout": "Ожидание подтверждения поступления посылки сотрудником пункта выдачи."

// tj
"tabNeedsAction": "Амал лозим",
"tabInFulfillment": "Дар ҷараён",
"tabCompleted": "Анҷом ёфта",
"awaitingArrivalCallout": "Дар интизори тасдиқи расидани бастаи коркунони нуқтаи таҳвил."
```

### Tests

**Backend** — fake adapter test for the new `statuses` param:

```
GetAdminOrdersWithStatusesFilterTest
  shouldReturnOnlyNeedsActionOrders_whenStatusesPAID_PENDING
  shouldReturnAllOrders_whenStatusesParamIsEmpty
  shouldIgnoreStatusParam_whenStatusesParamProvided
```

Fake: `InMemoryOrderRepository` implements `LoadOrdersPort`. Pre-load 3 orders in PENDING, SHIPPED, DELIVERED. Assert page results match the `statuses` filter.

**Frontend** — TypeScript + lint:

```bash
npx tsc --noEmit --prefix frontend
npm run lint --prefix frontend
```

### Verification

1. Navigate to `/manage/orders`.
2. Confirm: three tabs render — "Needs Action", "In Fulfillment", "Completed". Default active is "Needs Action".
3. Confirm: orders shown match the tab's status group (no DELIVERED orders in Needs Action tab).
4. Switch to "In Fulfillment" — confirm URL updates to `?tab=in-fulfillment`. Reload the page — confirm tab remains In Fulfillment.
5. Search for a customer name — confirm results filter within the active tab's status group.
6. Navigate to an order detail for a PAID pickpoint order. Confirm: "Advance" button is absent (PAID → READY_FOR_PICKUP bypass removed). Confirm: Ship button appears for PAID HOME orders (unchanged).
7. Navigate to an order detail for a SHIPPED pickpoint order. Confirm: blue "Awaiting pickup point staff…" callout is visible.
8. Navigate to an order detail for a SHIPPED HOME order. Confirm: callout does NOT appear.

---

## Phase 2 — Admin Order Detail Enhancements

**Goal:** Show customer return information and pickpoint arrival metadata directly on the admin order detail view.

**Backend prerequisite:** Two small backend additions:
- `GET /api/v1/admin/orders/{id}/customer-returns` — returns a list of `CustomerReturnListDto` for the order.
- `AdminOrderDto` extended with `pickpointArrivedAt: Instant?` and `pickpointConfirmedByUserName: String?`.

### 2a — Extract `SectionCard` to shared UI

**`frontend/src/shared/ui/SectionCard.tsx`** (Create)

Extract the existing local definition from `AdminOrderDetailView.tsx` lines 58–67:

```tsx
import { cn } from "@/shared/lib/utils";

interface SectionCardProps {
  title: string;
  children: React.ReactNode;
  className?: string;
}

export function SectionCard({ title, children, className }: SectionCardProps) {
  return (
    <div className={cn("rounded-xl border bg-card p-5", className)}>
      <h3 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wide">
        {title}
      </h3>
      {children}
    </div>
  );
}
```

Export from `shared/ui/index.ts` (or add to existing barrel). Remove the duplicate local definition from `AdminOrderDetailView.tsx` and import from `@/shared/ui`.

### 2b — Backend: Add `pickpointArrivedAt` + `pickpointConfirmedByUserName` to admin order DTO

**`backend/src/main/java/tj/radolfa/infrastructure/web/dto/AdminOrderDto.java`** (Modify)

Add two nullable fields to the record:

```java
Instant pickpointArrivedAt,
String pickpointConfirmedByUserName
```

These are populated in the controller's `toAdminDto(Order)` mapping method from `order.getPickpointArrivedAt()` and a user lookup by `order.getPickpointConfirmedByUserId()`.

**`backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/OrderJpaAdapter.java`** (No change needed if `Order` domain already has these fields after File 2 Phase 2's recall work — verify.)

### 2c — Backend: New endpoint for per-order customer returns

**`backend/src/main/java/tj/radolfa/infrastructure/web/AdminOrderController.java`** (Modify)

Add below the `/refund` endpoint:

```java
@GetMapping("/{id}/customer-returns")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public List<CustomerReturnListDto> getCustomerReturnsForOrder(@PathVariable Long id) {
    return getCustomerReturnsForOrderUseCase.execute(id);
}
```

**`backend/src/main/java/tj/radolfa/application/ports/in/GetCustomerReturnsForOrderUseCase.java`** (Create)

```java
public interface GetCustomerReturnsForOrderUseCase {
    List<CustomerReturn> execute(Long orderId);
}
```

**`backend/src/main/java/tj/radolfa/application/services/GetCustomerReturnsForOrderService.java`** (Create)

```java
@Service
@RequiredArgsConstructor
public class GetCustomerReturnsForOrderService implements GetCustomerReturnsForOrderUseCase {
    private final LoadCustomerReturnPort loadCustomerReturnPort;

    @Override
    public List<CustomerReturn> execute(Long orderId) {
        return loadCustomerReturnPort.findByOrderId(orderId);
    }
}
```

**`backend/src/main/java/tj/radolfa/application/ports/out/LoadCustomerReturnPort.java`** (Modify)

Add:

```java
List<CustomerReturn> findByOrderId(Long orderId);
```

### 2d — Frontend: Extend `AdminOrderDetail` type

**`frontend/src/entities/order/model/types.ts`** (Modify)

Add to `AdminOrderDetail` interface:

```ts
// Pickpoint arrival (populated once order reaches READY_FOR_PICKUP)
pickpointArrivedAt: string | null;
pickpointConfirmedByUserName: string | null;
// Customer return attached to this order (null if none)
customerReturn: AdminOrderDetailReturn | null;
```

Add the `AdminOrderDetailReturn` interface (can share `CustomerReturn` from entities/pickpoint or define inline):

```ts
export interface AdminOrderDetailReturn {
  id: number;
  status: CustomerReturnStatus;
  reason: string;
  receivedAt: string | null;
  sentToWarehouseAt: string | null;
  totalRefundAmount: number | null;
  items: Array<{
    productName: string;
    quantity: number;
    price: number;
    resellability: "PENDING_REVIEW" | "RESELLABLE" | "DEFECTIVE" | null;
  }>;
}
```

### 2e — Frontend: Add `useAdminCustomerReturnsForOrder` hook

**`frontend/src/features/order-management/api.ts`** (Modify)

The hook queries `/api/v1/admin/orders/{id}/customer-returns` — an admin order endpoint. It belongs in `features/order-management`, not `features/customer-return-management`. Placing it in `customer-return-management` and then importing it into `order-management` would be a cross-slice import at the same FSD layer, which is forbidden.

Add to `features/order-management/api.ts`:

```ts
export function useAdminCustomerReturnsForOrder(orderId: number) {
  return useQuery({
    queryKey: ["admin-order-customer-returns", orderId],
    queryFn: () =>
      apiClient
        .get<AdminOrderDetailReturn[]>(`/api/v1/admin/orders/${orderId}/customer-returns`)
        .then((r) => r.data),
    enabled: !!orderId,
  });
}
```

Import `AdminOrderDetailReturn` from `@/entities/order/model/types` (defined in step 2d).

### 2f — Add "Customer Returns" section to `AdminOrderDetailView`

**`frontend/src/features/order-management/ui/AdminOrderDetailView.tsx`** (Modify)

1. Import `useAdminCustomerReturnsForOrder` from `../api` (within the same `features/order-management` slice).
2. Call the hook: `const { data: returns = [] } = useAdminCustomerReturnsForOrder(order.id);`
3. Add `SectionCard` below `OrderItemsStockTable` (hidden when empty):

```tsx
{returns.length > 0 && (
  <SectionCard title={t("detail.customerReturns")}>
    <div className="space-y-3">
      {returns.map((ret) => (
        <div key={ret.id} className="flex items-center justify-between text-sm">
          <div className="space-y-0.5">
            <p className="font-medium">{t("detail.returnLabel")} #{ret.id}</p>
            <p className="text-xs text-muted-foreground">
              {ret.items.length} {t("detail.items")} ·{" "}
              {t(`detail.returnStatus.${ret.status}`)}
            </p>
          </div>
          {ret.totalRefundAmount != null && (
            <span className="text-sm font-medium text-green-700">
              {formatPrice(ret.totalRefundAmount)}
            </span>
          )}
        </div>
      ))}
    </div>
  </SectionCard>
)}
```

### 2g — Add "Pickup Point Activity" block for `READY_FOR_PICKUP+` orders

**`frontend/src/features/order-management/ui/AdminOrderDetailView.tsx`** (Modify)

Inside the existing Delivery `SectionCard` (after pickpoint name + address), add arrival metadata when available:

```tsx
{order.deliveryType === "PICKPOINT" && (
  <>
    {order.pickpointName && <InfoRow label={t("detail.pickupPoint")} value={order.pickpointName} />}
    {order.pickpointAddress && <InfoRow label={t("detail.pickupAddress")} value={order.pickpointAddress} />}
    {order.pickpointArrivedAt && (
      <InfoRow
        label={t("detail.arrivedAt")}
        value={formatDate(order.pickpointArrivedAt)}
      />
    )}
    {order.pickpointConfirmedByUserName && (
      <InfoRow
        label={t("detail.confirmedBy")}
        value={order.pickpointConfirmedByUserName}
      />
    )}
    {/* Overdue badge */}
    {order.status === "READY_FOR_PICKUP" && isPickpointOverdue(order) && (
      <span className="mt-1 inline-flex items-center rounded-full bg-red-100 px-2 py-0.5 text-xs text-red-700">
        {t("detail.pickpointOverdue")}
      </span>
    )}
  </>
)}
```

`isPickpointOverdue(order)` is a pure function: returns `true` when `pickpointArrivedAt` is more than N days ago (use the same `radolfa.pickpoint.storage-days` config value used by the pickpoint storage expiry job).

### 2h — Add i18n keys

**All three locale files** — under `"manage.orders"` namespace:

```json
// en
"detail.customerReturns": "Customer Returns",
"detail.returnLabel": "Return",
"detail.items": "items",
"detail.arrivedAt": "Arrived at Pickup Point",
"detail.confirmedBy": "Confirmed by",
"detail.pickupAddress": "Pickup Address",
"detail.pickpointOverdue": "Overdue",
"detail.returnStatus.RECEIVED": "Received",
"detail.returnStatus.SENT_TO_WAREHOUSE": "Sent to Warehouse",
"detail.returnStatus.REFUND_APPROVED": "Refund Approved",
"detail.returnStatus.REFUNDED": "Refunded"

// ru
"detail.customerReturns": "Возвраты",
"detail.returnLabel": "Возврат",
"detail.items": "товаров",
"detail.arrivedAt": "Поступило в пункт выдачи",
"detail.confirmedBy": "Подтвердил",
"detail.pickupAddress": "Адрес пункта выдачи",
"detail.pickpointOverdue": "Просрочено",
"detail.returnStatus.RECEIVED": "Принят",
"detail.returnStatus.SENT_TO_WAREHOUSE": "Отправлен на склад",
"detail.returnStatus.REFUND_APPROVED": "Возврат одобрен",
"detail.returnStatus.REFUNDED": "Средства возвращены"

// tj
"detail.customerReturns": "Баргардониҳо",
"detail.returnLabel": "Баргардонӣ",
"detail.items": "мол",
"detail.arrivedAt": "Ба нуқтаи таҳвил расид",
"detail.confirmedBy": "Тасдиқ кард",
"detail.pickupAddress": "Суроғаи нуқтаи таҳвил",
"detail.pickpointOverdue": "Мӯҳлат гузашт",
"detail.returnStatus.RECEIVED": "Қабул шуд",
"detail.returnStatus.SENT_TO_WAREHOUSE": "Ба анбор фиристода шуд",
"detail.returnStatus.REFUND_APPROVED": "Баргардонидан тасдиқ шуд",
"detail.returnStatus.REFUNDED": "Маблағ баргардонида шуд"
```

### Tests

**Backend:**

```
GetCustomerReturnsForOrderServiceTest
  shouldReturnEmptyList_whenNoReturnsExist
  shouldReturnReturns_whenOrderHasReturn
```

Fake: `InMemoryLoadCustomerReturnPort` — seeded with two `CustomerReturn` records for different order IDs. Assert only the matching orderId is returned.

**Frontend:**

```bash
npx tsc --noEmit --prefix frontend
npm run lint --prefix frontend
```

### Verification

1. Navigate to the detail of an order that has an associated customer return.
2. Confirm: "Customer Returns" section appears with the return ID, item count, status, and refund amount.
3. Navigate to the detail of an order with no return. Confirm: section is absent.
4. Navigate to a READY_FOR_PICKUP pickpoint order that has `pickpointArrivedAt` set.
5. Confirm: "Arrived at Pickup Point" row shows the formatted timestamp.
6. Confirm: "Confirmed by" row shows the staff member's name.
7. Create a test order that arrived >N days ago. Confirm: red "Overdue" badge appears.
8. Confirm `SectionCard` is used consistently — no duplicated inline card definitions remain in `AdminOrderDetailView.tsx`.

---

## Phase 3 — Pickpoint Staff Dashboard Improvements

**Goal:** `IncomingPackageCard` shows item details; multi-select with a sticky batch confirm bar reduces repetitive tapping; the tab bar scrolls horizontally instead of cramming into 5 equal columns.

**Backend prerequisite:** `PickpointOrderDto` (Java) must be extended with `totalItemCount: int`, `totalWeightKg: BigDecimal`, and `items: List<PickpointOrderItemDto>`. Sync to TS via `/bridge` skill after DTO change. The frontend plan below assumes these fields exist.

### 3a — Extend `PickpointOrder` TS type

**`frontend/src/entities/user/model/types.ts`** (Modify)

Extend `PickpointOrder`:

```ts
export interface PickpointOrderItem {
  productName: string;
  skuCode: string;
  sizeLabel: string | null;
  quantity: number;
  imageUrl: string | null;
}

export interface PickpointOrder {
  orderId: number;
  customerFirstName: string;
  customerPhone: string;
  status: OrderStatus;
  readyAt: string | null;
  expiresAt: string | null;
  daysUntilExpiry: number;
  overdue: boolean;
  daysOverdue: number;
  // New fields:
  totalItemCount: number;
  totalWeightKg: number | null;
  items: PickpointOrderItem[];
}
```

### 3b — Expand `IncomingPackageCard` with item details

**`frontend/src/features/pickpoint/ui/IncomingPackageCard.tsx`** (Modify)

Replace the minimal card body with an expanded layout. Logic steps:

1. Show `totalItemCount` + `totalWeightKg` in a summary row below the customer name.
2. Show a product list (collapsed at >3 items — toggle with "Show all" link).
3. Add a Shadcn `<Checkbox>` in the top-right corner of the card (controlled by parent).
4. Remove the per-card "Confirm Arrival" button — batch confirm replaces it.

```tsx
import Image from "next/image";

interface IncomingPackageCardProps {
  order: PickpointOrder;
  selected: boolean;
  onToggle: (orderId: number) => void;
}

const COLLAPSED_ITEM_LIMIT = 3;

export function IncomingPackageCard({ order, selected, onToggle }: IncomingPackageCardProps) {
  const t = useTranslations("pickpoint");
  const [expanded, setExpanded] = useState(false);
  const visibleItems = expanded ? order.items : order.items.slice(0, COLLAPSED_ITEM_LIMIT);

  return (
    <div className={cn(
      "rounded-xl border bg-card p-4 transition-colors",
      selected && "border-primary/50 bg-primary/5"
    )}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-semibold">{t("orderLabel")} #{order.orderId}</p>
          <p className="text-xs text-muted-foreground">{order.customerFirstName}</p>
        </div>
        <Checkbox
          checked={selected}
          onCheckedChange={() => onToggle(order.orderId)}
          aria-label={t("selectOrder")}
        />
      </div>

      {/* Summary row */}
      <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
        <span>{order.totalItemCount} {t("items")}</span>
        {order.totalWeightKg != null && (
          <span>{order.totalWeightKg} kg</span>
        )}
      </div>

      {/* Product list */}
      {order.items.length > 0 && (
        <div className="mt-2 space-y-1">
          {visibleItems.map((item, i) => (
            <div key={i} className="flex items-center gap-2 text-xs">
              {item.imageUrl && (
                <Image
                  src={item.imageUrl}
                  alt={item.productName}
                  width={24}
                  height={24}
                  unoptimized
                  className="rounded object-cover"
                />
              )}
              <span className="flex-1 truncate">{item.productName}</span>
              {item.sizeLabel && (
                <span className="shrink-0 text-muted-foreground">{item.sizeLabel}</span>
              )}
              <span className="shrink-0">× {item.quantity}</span>
            </div>
          ))}
          {order.items.length > COLLAPSED_ITEM_LIMIT && (
            <button
              className="text-xs text-primary hover:underline"
              onClick={() => setExpanded((v) => !v)}
            >
              {expanded
                ? t("showLess")
                : t("showMore", { count: order.items.length - COLLAPSED_ITEM_LIMIT })}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
```

### 3c — Add Set-based multi-select state + sticky batch confirm bar

**`frontend/src/features/pickpoint/ui/PickpointDashboardPage.tsx`** (Modify)

1. Add selection state in the `incoming` tab section:
   ```ts
   const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
   const confirmArrival = useConfirmArrival();

   const toggleOrder = (id: number) =>
     setSelectedIds((prev) => {
       const next = new Set(prev);
       next.has(id) ? next.delete(id) : next.add(id);
       return next;
     });
   ```

2. Pass `selected` + `onToggle` to each `IncomingPackageCard`:
   ```tsx
   {incomingOrders.map((o) => (
     <IncomingPackageCard
       key={o.orderId}
       order={o}
       selected={selectedIds.has(o.orderId)}
       onToggle={toggleOrder}
     />
   ))}
   ```

3. Add sticky bottom bar that renders only when `selectedIds.size > 0`. Uses `fixed bottom-0` pattern (mobile-first):
   ```tsx
   {selectedIds.size > 0 && (
     <div className="fixed bottom-0 left-0 right-0 z-40 border-t bg-background p-3 shadow-lg">
       <div className="mx-auto flex max-w-lg items-center justify-between">
         <span className="text-sm text-muted-foreground">
           {t("selectedCount", { count: selectedIds.size })}
         </span>
         <Button
           size="sm"
           disabled={confirmArrival.isPending}
           onClick={async () => {
             await Promise.all([...selectedIds].map((id) => confirmArrival.mutateAsync(id)));
             setSelectedIds(new Set());
           }}
         >
           {confirmArrival.isPending
             ? t("confirming")
             : t("confirmSelected", { count: selectedIds.size })}
         </Button>
       </div>
     </div>
   )}
   ```

4. Clear `selectedIds` when the tab changes.

### 3d — Replace `grid-cols-5` with scrollable tab bar

**`frontend/src/features/pickpoint/ui/PickpointDashboardPage.tsx`** (Modify)

Find the `<TabsList className="w-full grid grid-cols-5">` (line ~121) and change to:

```tsx
<TabsList className="h-auto w-full justify-start gap-1 overflow-x-auto bg-transparent p-0 scrollbar-hide">
  {/* Each trigger needs shrink-0 to prevent squishing */}
  <TabsTrigger value="incoming" className="shrink-0 whitespace-nowrap rounded-full border px-3 py-1.5 text-xs data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
    {t("tab.incoming")} <TabBadge count={incomingCount} />
  </TabsTrigger>
  {/* ...repeat for each of the 5 tabs */}
```

Mirror `ReviewVariantFilterStrip.tsx:21` exactly: `overflow-x-auto pb-1 scrollbar-hide` + `shrink-0 whitespace-nowrap` on each item.

### 3e — Add `pickpoint` i18n namespace

**All three locale files** — add new top-level `"pickpoint"` namespace:

```json
// en
"pickpoint": {
  "orderLabel": "Order",
  "selectOrder": "Select order",
  "items": "items",
  "showMore": "+{count} more",
  "showLess": "Show less",
  "selectedCount": "{count} selected",
  "confirmSelected": "Confirm Arrival ({count})",
  "confirming": "Confirming…",
  "tab": {
    "incoming": "Incoming",
    "awaiting": "Awaiting",
    "returns": "Returns",
    "customerReturns": "Walk-in Returns",
    "history": "History"
  },
  "confirmArrival": "Confirm Arrival",
  "readySince": "Ready since {date}",
  "overdue": "Overdue · {days}d",
  "noOrders": "No orders in this category.",
  "awaitingPickup": "Awaiting customer pickup",
  "enRoute": "En route — awaiting arrival confirmation"
}

// ru — same keys, Russian values
"pickpoint": {
  "orderLabel": "Заказ",
  "selectOrder": "Выбрать заказ",
  "items": "товаров",
  "showMore": "+{count} ещё",
  "showLess": "Свернуть",
  "selectedCount": "{count} выбрано",
  "confirmSelected": "Подтвердить прибытие ({count})",
  "confirming": "Подтверждение…",
  "tab": {
    "incoming": "Входящие",
    "awaiting": "Ожидают",
    "returns": "Возвраты",
    "customerReturns": "Самовозврат",
    "history": "История"
  },
  "confirmArrival": "Подтвердить прибытие",
  "readySince": "Готово с {date}",
  "overdue": "Просрочено · {days}д",
  "noOrders": "Заказов в этой категории нет.",
  "awaitingPickup": "Ожидает получения",
  "enRoute": "В пути — ожидание подтверждения прибытия"
}
```

*(tj locale — same structure with Tajik values)*

### Tests

- TypeScript check: `npx tsc --noEmit --prefix frontend` — confirms `IncomingPackageCard` new props, `selected` + `onToggle` threading.
- Lint: `npm run lint --prefix frontend`.

### Verification

1. Log in as PICKPOINT_STAFF. Navigate to the pickpoint dashboard.
2. Confirm: 5 tabs are scrollable and do not squish — test on a 375px-wide viewport (iPhone SE).
3. Click the "Incoming" tab. Confirm: each package card shows item count + weight + product list.
4. For a card with >3 items: confirm list is collapsed initially; "+N more" link expands it; "Show less" collapses.
5. Tap the checkbox on 2 cards. Confirm: sticky bar appears with "Confirm Arrival (2)" button.
6. Tap "Confirm Arrival (2)". Confirm: both orders move to the "Awaiting" tab; sticky bar disappears; no JavaScript error.
7. Confirm existing per-card confirm button is gone — single-click confirmation is now only via the sticky bar.

---

## Phase 4 — Courier Dashboard Server-Side Wiring

**Goal:** Replace the three client-side `.filter()` calls in `CourierDashboardPage` with three independent server-side paginated queries — one per status tab — and add pagination controls.

**Backend prerequisite:** File 2 Phase 1 must be complete (`GET /api/v1/courier/orders` must accept `statuses` and `page`/`size` params and return `PaginatedResponse<CourierOrder>`).

### 4a — Refactor `useCourierOrders` hook

**`frontend/src/features/courier/api.ts`** (Modify)

Replace the parameterless hook with a parameterized one:

```ts
// Before:
export function useCourierOrders() {
  return useQuery({
    queryKey: ["courier-orders"],
    queryFn: () => apiClient.get<CourierOrder[]>("/api/v1/courier/orders").then(r => r.data),
  });
}

// After:
export function useCourierOrders(statuses: OrderStatus[], page: number, size: number = 10) {
  return useQuery({
    queryKey: ["courier-orders", statuses.join(","), page, size],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<CourierOrder>>("/api/v1/courier/orders", {
          params: { statuses: statuses.join(","), page, size },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
    enabled: statuses.length > 0,
  });
}
```

Add imports:
```ts
import { keepPreviousData } from "@tanstack/react-query";
import type { PaginatedResponse } from "@/shared/api/types";
import type { OrderStatus } from "@/entities/order/model/types";
```

### 4b — Refactor `CourierDashboardPage` tabs to use per-tab queries

**`frontend/src/features/courier/ui/CourierDashboardPage.tsx`** (Modify)

1. Define tab configuration:
   ```ts
   const COURIER_TABS = [
     { value: "collect",  statuses: ["SHIPPED"]             as OrderStatus[], label: "tab.toCollect"  },
     { value: "transit",  statuses: ["OUT_FOR_DELIVERY"]    as OrderStatus[], label: "tab.inTransit"  },
     { value: "attempted",statuses: ["DELIVERY_ATTEMPTED"]  as OrderStatus[], label: "tab.attempted"  },
   ] as const;
   ```

2. Remove the three `.filter()` calls entirely.

3. Add pagination state per tab:
   ```ts
   const [pages, setPages] = useState<Record<string, number>>({
     collect: 1, transit: 1, attempted: 1,
   });
   ```

4. Render each tab panel using its own `useCourierOrders` hook call (TanStack Query deduplicates identical keys):
   ```tsx
   {COURIER_TABS.map(({ value, statuses, label }) => {
     const page = pages[value];
     return (
       <TabsContent key={value} value={value}>
         <CourierTabPanel
           statuses={statuses}
           page={page}
           onPageChange={(p) => setPages((prev) => ({ ...prev, [value]: p }))}
         />
       </TabsContent>
     );
   })}
   ```

5. `CourierTabPanel` is a small sub-component (or inline) that calls `useCourierOrders(statuses, page)` and renders the list + pagination controls.

### 4c — Add pagination controls per tab

In each tab panel (or `CourierTabPanel`), add the same pagination UI as `CustomerReturnsQueuePage.tsx:180-203`:

```tsx
const { data, isLoading } = useCourierOrders(statuses, page);

{/* List */}
{data?.content.map((order) => (
  <CourierOrderCard key={order.orderId} order={order} />
))}

{/* Pagination */}
{data && data.totalPages > 1 && (
  <div className="flex items-center justify-between text-sm text-muted-foreground py-2">
    <span>
      {t("showing")} {(page - 1) * 10 + 1}–
      {Math.min(page * 10, data.totalElements)} {t("of")} {data.totalElements}
    </span>
    <div className="flex gap-2">
      <Button variant="outline" size="sm"
        onClick={() => onPageChange(page - 1)} disabled={data.first}>
        <ChevronLeft className="h-4 w-4" />
      </Button>
      <Button variant="outline" size="sm"
        onClick={() => onPageChange(page + 1)} disabled={data.last}>
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  </div>
)}
```

### 4d — Add `courier` i18n namespace

**All three locale files** — add new top-level `"courier"` namespace:

```json
// en
"courier": {
  "title": "My Deliveries",
  "tab": {
    "toCollect": "To Collect",
    "inTransit": "In Transit",
    "attempted": "Attempted"
  },
  "showing": "Showing",
  "of": "of",
  "noOrders": "No orders in this category.",
  "collectOrder": "I've Collected This Order",
  "recordAttempt": "Record Another Attempt",
  "attemptReason": "Attempt reason",
  "attemptPhoto": "Upload photo (optional)"
}

// ru — same keys
"courier": {
  "title": "Мои доставки",
  "tab": {
    "toCollect": "Забрать",
    "inTransit": "В пути",
    "attempted": "Попытка совершена"
  },
  "showing": "Показано",
  "of": "из",
  "noOrders": "Заказов в этой категории нет.",
  "collectOrder": "Я забрал этот заказ",
  "recordAttempt": "Зафиксировать ещё одну попытку",
  "attemptReason": "Причина",
  "attemptPhoto": "Фото (необязательно)"
}
```

*(tj locale — same structure with Tajik values)*

### Tests

- TypeScript: `npx tsc --noEmit --prefix frontend` — confirms `useCourierOrders` new signature propagates through `CourierDashboardPage` and `CourierTabPanel` without type errors.
- Lint: `npm run lint --prefix frontend` — confirms no `content[]` `.filter()` calls remain in courier files.

### Verification

1. Deploy File 2 Phase 1 backend. Confirm `GET /api/v1/courier/orders?statuses=SHIPPED&page=1&size=10` returns a `PaginatedResponse<CourierOrder>`.
2. Log in as COURIER.
3. Navigate to `/courier`.
4. Confirm: three tabs render — "To Collect", "In Transit", "Attempted".
5. Confirm: "To Collect" tab shows only orders with status `SHIPPED`. "In Transit" shows `OUT_FOR_DELIVERY`. "Attempted" shows `DELIVERY_ATTEMPTED`.
6. Confirm: no JavaScript error about calling `.filter()` on undefined (the old bug).
7. Create a test courier with >10 SHIPPED orders. Confirm: pagination controls appear in "To Collect" tab, page 2 loads different orders.
8. Switch tabs while on page 2 of "To Collect". Confirm: "In Transit" tab starts at page 1 (independent per-tab pagination).
9. Switch locale to `ru`. Confirm: tab labels, "Showing X–Y of Z", action buttons all render in Russian.

---

## Post-Implementation Checklist

- [ ] `npx tsc --noEmit --prefix frontend` passes with zero errors across all four phases.
- [ ] `npm run lint --prefix frontend` passes with zero violations.
- [ ] No `.filter()`, `.sort()`, or `.slice()` calls on paginated `content[]` remain in any courier, pickpoint, or admin file.
- [ ] Admin order list defaults to "Needs Action" tab; URL updates on tab switch; page reloads preserve the active tab.
- [ ] `nextStatusFor()` in `AdminOrderDetailView.tsx` — `case "PAID"` returns `null` for all delivery types. Confirm the Ship button still appears for PAID HOME orders (the `ShipOrderModal` is shown separately and is not gated by `nextStatusFor()`).
- [ ] `SectionCard` has been extracted to `shared/ui/SectionCard.tsx` — no duplicate local definition remains in `AdminOrderDetailView.tsx`.
- [ ] Customer Returns section in admin order detail is hidden when the order has no returns — confirmed for 3 test orders with/without returns.
- [ ] Pickpoint arrival metadata (`arrivedAt`, `confirmedBy`, overdue badge) only renders when the fields are non-null.
- [ ] `IncomingPackageCard` — per-card confirm button is removed; batch confirm via sticky bar is the only confirmation path.
- [ ] Sticky bar `fixed bottom-0` does not overlap the bottom nav on iOS Safari — test on a physical or emulated iPhone.
- [ ] Pickpoint tab bar is horizontally scrollable on 375px viewport with no text truncation or overflow.
- [ ] `PickpointDashboardPage` and `CourierDashboardPage` have zero hardcoded English strings — all routed through `useTranslations("pickpoint")` / `useTranslations("courier")`.
- [ ] File 2 Phase 1 (courier endpoint) cross-check: `GET /api/v1/courier/orders?statuses=SHIPPED&page=1&size=10` returns the expected `PaginatedResponse<CourierOrder>` shape matching the `CourierOrder` TypeScript type.
- [ ] File 2 Phase 2 cross-check: `RECALL_REQUESTED` status renders a human-readable label in the admin order list and detail view (uses `manage.orders.status.RECALL_REQUESTED` key added in File 4 Phase 1).
