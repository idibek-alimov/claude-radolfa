"use client";

import { useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { Home, MapPin, Package, Truck } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
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
  const { data: order, isLoading } = useAdminOrder(orderId);
  const updateStatus = useUpdateOrderStatus();

  const [courierName, setCourierName]                   = useState("");
  const [trackingNumber, setTrackingNumber]             = useState("");
  const [estimatedDeliveryDate, setEstimatedDeliveryDate] = useState("");
  const [courierError, setCourierError]                 = useState(false);

  function handleStatusChange(newStatus: OrderStatus) {
    updateStatus.mutate(
      { orderId, status: newStatus },
      {
        onSuccess: () => toast.success("Order status updated."),
        onError: (err) => toast.error(getErrorMessage(err, "Failed to update status.")),
      }
    );
  }

  function handleMarkShipped() {
    if (!courierName.trim()) { setCourierError(true); return; }
    setCourierError(false);
    updateStatus.mutate(
      {
        orderId,
        status: "SHIPPED",
        courierName: courierName.trim(),
        trackingNumber: trackingNumber.trim() || undefined,
        estimatedDeliveryDate: estimatedDeliveryDate || undefined,
      },
      {
        onSuccess: () => {
          toast.success("Order marked as shipped.");
          setCourierName(""); setTrackingNumber(""); setEstimatedDeliveryDate("");
        },
        onError: (err) => toast.error(getErrorMessage(err, "Failed to ship order.")),
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

  const nextStatus     = nextStatusFor(order);
  const showShipForm   = order.status === "PAID" && order.deliveryType === "HOME";

  // ── Render ───────────────────────────────────────────────────────────────

  return (
    <div className="flex flex-col flex-1 gap-6">
      {/* Breadcrumb */}
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/manage/orders">Orders</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage>Order #{order.id}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      {/* Header */}
      <div className="flex items-center gap-3 flex-wrap">
        <h1 className="text-2xl font-bold tracking-tight">Order #{order.id}</h1>
        <OrderStatusBadge status={order.status} />
        <p className="text-sm text-muted-foreground ml-auto">
          {order.userPhone}
          {order.userName && <span className="ml-1">· {order.userName}</span>}
        </p>
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
              Summary
            </p>
            {order.loyaltyPointsRedeemed > 0 && (
              <div className="flex justify-between text-xs text-amber-700">
                <span>Loyalty discount</span>
                <span>−{(order.loyaltyPointsRedeemed * 0.01).toFixed(2)} TJS</span>
              </div>
            )}
            <div className="flex justify-between text-sm font-semibold pt-2 border-t">
              <span>Total</span>
              <span className="tabular-nums">{order.totalAmount.toFixed(2)} TJS</span>
            </div>
          </div>
        </div>

        {/* ── Sidebar ─────────────────────────────────────────────────────── */}
        <div className="space-y-4">

          {/* Customer */}
          <SectionCard title="Customer">
            <div className="text-sm">
              <p className="font-medium font-mono">{order.userPhone}</p>
              {order.userName && (
                <p className="text-muted-foreground text-xs mt-0.5">{order.userName}</p>
              )}
            </div>
          </SectionCard>

          {/* Delivery */}
          <SectionCard title="Delivery">
            {order.deliveryType === "HOME" ? (
              <div className="flex items-start gap-2">
                <Home className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                <div className="space-y-0.5 text-sm">
                  <p className="font-medium">Home Delivery</p>
                  {order.deliveryAddress && (
                    <p className="text-xs text-muted-foreground">{order.deliveryAddress}</p>
                  )}
                  {order.preferredTimeWindow && (
                    <p className="text-xs text-muted-foreground">
                      Time window: {order.preferredTimeWindow}
                    </p>
                  )}
                </div>
              </div>
            ) : order.deliveryType === "PICKPOINT" ? (
              <div className="flex items-start gap-2">
                <MapPin className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                <div className="space-y-0.5 text-sm">
                  <p className="font-medium">{order.pickpointName ?? "Pickup Point"}</p>
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
            <SectionCard title="Shipment">
              <div className="flex items-center gap-2 mb-1">
                <Truck className="h-4 w-4 text-primary" />
                <span className="text-sm font-medium">{order.courierName}</span>
              </div>
              <div className="space-y-1 text-xs">
                {order.trackingNumber && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Tracking</span>
                    <span className="font-mono font-medium">{order.trackingNumber}</span>
                  </div>
                )}
                {order.estimatedDeliveryDate && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">ETA</span>
                    <span>{new Date(order.estimatedDeliveryDate).toLocaleDateString()}</span>
                  </div>
                )}
              </div>
            </SectionCard>
          )}

          {/* Loyalty */}
          {(order.loyaltyPointsRedeemed > 0 || order.loyaltyPointsAwarded > 0) && (
            <SectionCard title="Loyalty Points">
              <div className="space-y-1 text-xs">
                {order.loyaltyPointsRedeemed > 0 && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Redeemed</span>
                    <span className="text-amber-700 font-medium">−{order.loyaltyPointsRedeemed} pts</span>
                  </div>
                )}
                {order.loyaltyPointsAwarded > 0 && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Awarded</span>
                    <span className="text-green-600 font-medium">+{order.loyaltyPointsAwarded} pts</span>
                  </div>
                )}
              </div>
            </SectionCard>
          )}

          {/* Status actions */}
          {(showShipForm || nextStatus) && (
            <SectionCard title="Status Actions">
              {showShipForm ? (
                <div className="space-y-3">
                  <div className="space-y-1.5">
                    <Label htmlFor="courier-name" className="text-xs">
                      Courier Name <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="courier-name"
                      value={courierName}
                      onChange={(e) => { setCourierName(e.target.value); setCourierError(false); }}
                      placeholder="e.g. DHL"
                      className={courierError ? "border-destructive" : ""}
                    />
                    {courierError && (
                      <p className="text-destructive text-xs">Courier name is required.</p>
                    )}
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="tracking-number" className="text-xs">Tracking Number</Label>
                    <Input
                      id="tracking-number"
                      value={trackingNumber}
                      onChange={(e) => setTrackingNumber(e.target.value)}
                      placeholder="Optional"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="edd" className="text-xs">Estimated Delivery Date</Label>
                    <Input
                      id="edd"
                      type="date"
                      value={estimatedDeliveryDate}
                      onChange={(e) => setEstimatedDeliveryDate(e.target.value)}
                    />
                  </div>
                  <Button
                    className="w-full"
                    onClick={handleMarkShipped}
                    disabled={updateStatus.isPending}
                  >
                    <Truck className="h-4 w-4 mr-2" />
                    Mark as Shipped
                  </Button>
                </div>
              ) : nextStatus ? (
                <Select
                  value=""
                  onValueChange={(v) => handleStatusChange(v as OrderStatus)}
                  disabled={updateStatus.isPending}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder={`Move to ${nextStatus}`} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={nextStatus}>
                      <div className="flex items-center gap-2">
                        <Package className="h-3.5 w-3.5" />
                        {nextStatus}
                      </div>
                    </SelectItem>
                  </SelectContent>
                </Select>
              ) : null}
            </SectionCard>
          )}

        </div>
      </div>
    </div>
  );
}
