"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Ban, Home, MapPin, Truck } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/shared/ui/breadcrumb";
import { OrderStatusBadge } from "@/entities/order/ui/OrderStatusBadge";
import { useAdminOrder, useUpdateOrderStatus } from "@/entities/order";
import { getErrorMessage } from "@/shared/lib";
import type { AdminOrderDetail, OrderStatus } from "@/entities/order";
import { FulfillmentTimeline } from "./FulfillmentTimeline";
import { OrderItemsStockTable } from "./OrderItemsStockTable";
import { ShipOrderModal } from "./ShipOrderModal";
import { CancelOrderModal } from "./CancelOrderModal";

function nextStatusFor(order: AdminOrderDetail): OrderStatus | null {
  const isPickpoint = order.deliveryType === "PICKPOINT";
  switch (order.status) {
    case "PENDING":          return "PAID";
    case "PAID":             return isPickpoint ? "READY_FOR_PICKUP" : "SHIPPED";
    case "SHIPPED":          return "DELIVERED";
    case "READY_FOR_PICKUP": return "DELIVERED";
    default:                 return null;
  }
}

// ── Section card ────────────────────────────────────────────────────────────

function SectionCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-xl border bg-card p-5 space-y-3">
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
        {title}
      </p>
      {children}
    </div>
  );
}

// ── Main component ───────────────────────────────────────────────────────────

interface Props {
  orderId: number;
}

export function AdminOrderDetailView({ orderId }: Props) {
  const t = useTranslations("manage.orders");
  const { data: order, isLoading } = useAdminOrder(orderId);
  const updateStatus = useUpdateOrderStatus();

  const [shipOpen, setShipOpen]     = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);

  function handleStatusChange(newStatus: OrderStatus) {
    updateStatus.mutate(
      { orderId, status: newStatus },
      {
        onSuccess: () => toast.success(t("toast.statusUpdated")),
        onError: (err) => toast.error(getErrorMessage(err, t("toast.statusUpdateFailed"))),
      }
    );
  }

  // ── Loading ──────────────────────────────────────────────────────────────

  if (isLoading || !order) {
    return (
      <div className="flex flex-col flex-1 gap-6">
        <Skeleton className="h-5 w-48" />
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-24 w-full" />
        <div className="grid grid-cols-1 lg:grid-cols-[1fr_340px] gap-6">
          <div className="space-y-3">
            {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}
          </div>
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-32 w-full" />)}
          </div>
        </div>
      </div>
    );
  }

  const nextStatus        = nextStatusFor(order);
  const isFinalState      = order.status === "DELIVERED" || order.status === "CANCELLED";
  const showShipButton    = order.status === "PAID" && order.deliveryType === "HOME";
  const showAdvanceButton = nextStatus !== null && !showShipButton;
  const showCancelButton  = !isFinalState;

  // ── Render ───────────────────────────────────────────────────────────────

  return (
    <div className="flex flex-col flex-1 gap-6">
      {/* Breadcrumb */}
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/manage/orders">{t("breadcrumb.orders")}</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage>{t("detail.title", { id: order.id })}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      {/* Header */}
      <div className="space-y-3">
        <div className="flex items-center gap-3 flex-wrap">
          <h1 className="text-2xl font-bold tracking-tight">{t("detail.title", { id: order.id })}</h1>
          <OrderStatusBadge status={order.status} />
          <p className="text-sm text-muted-foreground ml-auto">
            {order.userPhone}
            {order.userName && <span className="ml-1">· {order.userName}</span>}
          </p>
        </div>

        {(showShipButton || showAdvanceButton || showCancelButton) && (
          <div className="flex items-center justify-end gap-2 flex-wrap">
            {showShipButton && (
              <Button onClick={() => setShipOpen(true)}>
                <Truck className="h-4 w-4 mr-2" />
                {t("drawer.markAsShipped")}
              </Button>
            )}
            {showAdvanceButton && (
              <Button
                onClick={() => handleStatusChange(nextStatus!)}
                disabled={updateStatus.isPending}
              >
                {t("detail.moveTo", {
                  status: t(`status.${nextStatus}` as Parameters<typeof t>[0]),
                })}
              </Button>
            )}
            {showCancelButton && (
              <Button variant="destructive" onClick={() => setCancelOpen(true)}>
                <Ban className="h-4 w-4 mr-2" />
                {t("detail.cancelOrder")}
              </Button>
            )}
          </div>
        )}
      </div>

      {/* Timeline */}
      <FulfillmentTimeline
        status={order.status}
        createdAt={order.createdAt}
        shippedAt={order.shippedAt}
        deliveredAt={order.deliveredAt}
        cancelledAt={order.cancelledAt}
        deliveryType={order.deliveryType}
      />

      {/* Two-column body */}
      <div className="grid grid-cols-1 lg:grid-cols-[1fr_340px] gap-6 items-start">

        {/* ── Main column ─────────────────────────────────────────────────── */}
        <div className="space-y-4">
          {/* Items */}
          <OrderItemsStockTable items={order.items} />

          {/* Financials */}
          <div className="rounded-xl border bg-card p-5 space-y-2">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-1">
              {t("detail.summary")}
            </p>
            {order.loyaltyPointsRedeemed > 0 && (
              <div className="flex justify-between text-xs text-amber-700">
                <span>{t("detail.loyaltyDiscount")}</span>
                <span>−{(order.loyaltyPointsRedeemed * 0.01).toFixed(2)} TJS</span>
              </div>
            )}
            <div className="flex justify-between text-sm font-semibold pt-2 border-t">
              <span>{t("detail.total")}</span>
              <span className="tabular-nums">{order.totalAmount.toFixed(2)} TJS</span>
            </div>
          </div>
        </div>

        {/* ── Sidebar ─────────────────────────────────────────────────────── */}
        <div className="space-y-4">

          {/* Customer */}
          <SectionCard title={t("detail.customer")}>
            <div className="text-sm">
              <p className="font-medium font-mono">{order.userPhone}</p>
              {order.userName && (
                <p className="text-muted-foreground text-xs mt-0.5">{order.userName}</p>
              )}
            </div>
          </SectionCard>

          {/* Delivery */}
          <SectionCard title={t("detail.delivery")}>
            {order.deliveryType === "HOME" ? (
              <div className="flex items-start gap-2">
                <Home className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                <div className="space-y-0.5 text-sm">
                  <p className="font-medium">{t("detail.homeDelivery")}</p>
                  {order.deliveryAddress && (
                    <p className="text-xs text-muted-foreground">{order.deliveryAddress}</p>
                  )}
                  {order.preferredTimeWindow && (
                    <p className="text-xs text-muted-foreground">
                      {t("detail.timeWindow", { value: order.preferredTimeWindow })}
                    </p>
                  )}
                </div>
              </div>
            ) : order.deliveryType === "PICKPOINT" ? (
              <div className="flex items-start gap-2">
                <MapPin className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                <div className="space-y-0.5 text-sm">
                  <p className="font-medium">{order.pickpointName ?? t("detail.pickupPoint")}</p>
                  {order.pickpointAddress && (
                    <p className="text-xs text-muted-foreground">{order.pickpointAddress}</p>
                  )}
                </div>
              </div>
            ) : (
              <p className="text-xs text-muted-foreground">—</p>
            )}
          </SectionCard>

          {/* Shipment info (read-only) */}
          {order.courierName && (
            <SectionCard title={t("detail.shipment")}>
              <div className="flex items-center gap-2 mb-1">
                <Truck className="h-4 w-4 text-primary" />
                <span className="text-sm font-medium">{order.courierName}</span>
              </div>
              <div className="space-y-1 text-xs">
                {order.trackingNumber && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">{t("detail.tracking")}</span>
                    <span className="font-mono font-medium">{order.trackingNumber}</span>
                  </div>
                )}
                {order.estimatedDeliveryDate && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">{t("detail.eta")}</span>
                    <span>{new Date(order.estimatedDeliveryDate).toLocaleDateString()}</span>
                  </div>
                )}
              </div>
            </SectionCard>
          )}

          {/* Loyalty */}
          {(order.loyaltyPointsRedeemed > 0 || order.loyaltyPointsAwarded > 0) && (
            <SectionCard title={t("detail.loyaltyPoints")}>
              <div className="space-y-1 text-xs">
                {order.loyaltyPointsRedeemed > 0 && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">{t("detail.redeemed")}</span>
                    <span className="text-amber-700 font-medium">
                      −{order.loyaltyPointsRedeemed} {t("detail.ptsShort")}
                    </span>
                  </div>
                )}
                {order.loyaltyPointsAwarded > 0 && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">{t("detail.awarded")}</span>
                    <span className="text-green-600 font-medium">
                      +{order.loyaltyPointsAwarded} {t("detail.ptsShort")}
                    </span>
                  </div>
                )}
              </div>
            </SectionCard>
          )}

        </div>
      </div>

      <ShipOrderModal open={shipOpen} onClose={() => setShipOpen(false)} orderId={orderId} />
      <CancelOrderModal open={cancelOpen} onClose={() => setCancelOpen(false)} orderId={orderId} />
    </div>
  );
}
