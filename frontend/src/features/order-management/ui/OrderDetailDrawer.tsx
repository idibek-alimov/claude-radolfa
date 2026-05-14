"use client";

import Image from "next/image";
import { useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { X, MapPin, Home, Package, Truck } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/shared/ui/sheet";
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
import { OrderStatusBadge } from "@/entities/order/ui/OrderStatusBadge";
import { useAdminOrder, useUpdateOrderStatus } from "@/entities/order";
import type { OrderStatus } from "@/entities/order";
import { getErrorMessage } from "@/shared/lib";

import type { AdminOrderDetail } from "@/entities/order";

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

interface Props {
  orderId: number | null;
  onOpenChange: (open: boolean) => void;
}

export function OrderDetailDrawer({ orderId, onOpenChange }: Props) {
  const t = useTranslations("manage.orders.drawer");
  const tStatus = useTranslations("manage.orders.status");

  const { data: order, isLoading } = useAdminOrder(orderId);
  const updateStatus = useUpdateOrderStatus();

  const [courierName, setCourierName] = useState("");
  const [trackingNumber, setTrackingNumber] = useState("");
  const [estimatedDeliveryDate, setEstimatedDeliveryDate] = useState("");
  const [courierError, setCourierError] = useState(false);

  function handleStatusChange(newStatus: OrderStatus) {
    if (!orderId) return;
    updateStatus.mutate(
      { orderId, status: newStatus },
      {
        onSuccess: () => toast.success(t("statusUpdated")),
        onError: (err) => toast.error(getErrorMessage(err, t("statusUpdateFailed"))),
      }
    );
  }

  function handleMarkShipped() {
    if (!orderId) return;
    if (!courierName.trim()) {
      setCourierError(true);
      return;
    }
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
          toast.success(t("statusUpdated"));
          setCourierName("");
          setTrackingNumber("");
          setEstimatedDeliveryDate("");
        },
        onError: (err) => toast.error(getErrorMessage(err, t("statusUpdateFailed"))),
      }
    );
  }

  const nextStatus = order ? nextStatusFor(order) : null;
  const showShipmentForm =
    order?.status === "PAID" && order?.deliveryType === "HOME";

  return (
    <Sheet open={orderId !== null} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-xl overflow-y-auto">
        {isLoading || !order ? (
          <div className="p-6 space-y-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : (
          <>
            <SheetHeader className="pb-4 border-b">
              <div className="flex items-center justify-between gap-3">
                <SheetTitle className="text-base font-semibold">
                  {t("title", { id: order.id })}
                </SheetTitle>
                <div className="flex items-center gap-2">
                  <OrderStatusBadge status={order.status} />
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 w-7 p-0"
                    onClick={() => onOpenChange(false)}
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              </div>
              {/* Customer */}
              <div className="text-xs text-muted-foreground mt-1">
                <span className="font-medium text-foreground">{order.userPhone}</span>
                {order.userName && (
                  <span className="ml-2 text-muted-foreground">· {order.userName}</span>
                )}
              </div>
            </SheetHeader>

            <div className="py-4 space-y-5">
              {/* Items */}
              <section>
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">
                  {t("items")}
                </p>
                <ul className="space-y-2">
                  {order.items.map((item, i) => (
                    <li key={i} className="flex items-center gap-3">
                      {item.imageUrl ? (
                        <Image
                          src={item.imageUrl}
                          alt={item.productName}
                          width={40}
                          height={40}
                          unoptimized
                          className="rounded-md object-cover w-10 h-10 border bg-muted shrink-0"
                        />
                      ) : (
                        <div className="w-10 h-10 rounded-md bg-muted border shrink-0 flex items-center justify-center">
                          <Package className="h-4 w-4 text-muted-foreground" />
                        </div>
                      )}
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium truncate">{item.productName}</p>
                        <p className="text-xs text-muted-foreground">
                          {item.sizeLabel && `${item.sizeLabel} · `}× {item.quantity}
                        </p>
                      </div>
                      <p className="text-sm font-semibold shrink-0">
                        {(item.price * item.quantity).toFixed(2)} TJS
                      </p>
                    </li>
                  ))}
                </ul>
              </section>

              {/* Financials */}
              <section className="rounded-xl border bg-muted/30 p-4 space-y-1.5">
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">
                  {t("subtotal")}
                </p>
                {order.loyaltyPointsRedeemed > 0 && (
                  <div className="flex justify-between text-xs text-amber-700">
                    <span>{t("pointsDiscount")}</span>
                    <span>−{(order.loyaltyPointsRedeemed * 0.01).toFixed(2)} TJS</span>
                  </div>
                )}
                <div className="flex justify-between text-sm font-semibold pt-1 border-t">
                  <span>{t("total")}</span>
                  <span>{order.totalAmount.toFixed(2)} TJS</span>
                </div>
              </section>

              {/* Delivery */}
              <section className="rounded-xl border bg-muted/30 p-4">
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">
                  {t("deliverySection")}
                </p>
                {order.deliveryType === "HOME" ? (
                  <div className="flex items-start gap-2">
                    <Home className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                    <div className="space-y-0.5">
                      <p className="text-sm font-medium">{t("deliveryHome")}</p>
                      {order.deliveryAddress && (
                        <p className="text-xs text-muted-foreground">
                          <span className="font-medium text-foreground">{t("deliveryAddress")}:</span>{" "}
                          {order.deliveryAddress}
                        </p>
                      )}
                      {order.preferredTimeWindow && (
                        <p className="text-xs text-muted-foreground">
                          <span className="font-medium text-foreground">{t("deliveryWindow")}:</span>{" "}
                          {order.preferredTimeWindow}
                        </p>
                      )}
                    </div>
                  </div>
                ) : order.deliveryType === "PICKPOINT" ? (
                  <div className="flex items-start gap-2">
                    <MapPin className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                    <div className="space-y-0.5">
                      <p className="text-sm font-medium">
                        {order.pickpointName ?? t("deliveryPickpoint")}
                      </p>
                      {order.pickpointAddress && (
                        <p className="text-xs text-muted-foreground">
                          <span className="font-medium text-foreground">{t("deliveryPickpointAddress")}:</span>{" "}
                          {order.pickpointAddress}
                        </p>
                      )}
                    </div>
                  </div>
                ) : (
                  <p className="text-xs text-muted-foreground">—</p>
                )}
              </section>

              {/* Shipment info (read-only, shown when courier data exists) */}
              {order.courierName && (
                <section className="rounded-xl border bg-muted/30 p-4">
                  <div className="flex items-center gap-2 mb-3">
                    <Truck className="h-4 w-4 text-primary" />
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
                      {t("shipmentSection")}
                    </p>
                  </div>
                  <div className="space-y-1.5 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">{t("courierName")}</span>
                      <span className="font-medium">{order.courierName}</span>
                    </div>
                    {order.trackingNumber && (
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">{t("trackingNumber")}</span>
                        <span className="font-medium font-mono text-xs">{order.trackingNumber}</span>
                      </div>
                    )}
                    {order.estimatedDeliveryDate && (
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">{t("estimatedDeliveryDate")}</span>
                        <span className="font-medium">
                          {new Date(order.estimatedDeliveryDate).toLocaleDateString()}
                        </span>
                      </div>
                    )}
                  </div>
                </section>
              )}

              {/* Status transition */}
              <section>
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">
                  {t("statusLabel")}
                </p>

                {showShipmentForm ? (
                  <div className="space-y-3 rounded-xl border bg-muted/30 p-4">
                    <div className="space-y-1.5">
                      <Label htmlFor="courier-name" className="text-xs">
                        {t("courierName")} <span className="text-destructive">*</span>
                      </Label>
                      <Input
                        id="courier-name"
                        value={courierName}
                        onChange={(e) => { setCourierName(e.target.value); setCourierError(false); }}
                        placeholder={t("courierNamePlaceholder")}
                        className={courierError ? "border-destructive" : ""}
                      />
                      {courierError && (
                        <p className="text-destructive text-xs">{t("courierNameRequired")}</p>
                      )}
                    </div>
                    <div className="space-y-1.5">
                      <Label htmlFor="tracking-number" className="text-xs">
                        {t("trackingNumber")}
                      </Label>
                      <Input
                        id="tracking-number"
                        value={trackingNumber}
                        onChange={(e) => setTrackingNumber(e.target.value)}
                        placeholder={t("trackingNumberPlaceholder")}
                      />
                    </div>
                    <div className="space-y-1.5">
                      <Label htmlFor="edd" className="text-xs">
                        {t("estimatedDeliveryDate")}
                      </Label>
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
                      {t("markAsShipped")}
                    </Button>
                  </div>
                ) : nextStatus ? (
                  <Select
                    value=""
                    onValueChange={(v) => handleStatusChange(v as OrderStatus)}
                    disabled={updateStatus.isPending}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder={tStatus(nextStatus)} />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={nextStatus}>{tStatus(nextStatus)}</SelectItem>
                    </SelectContent>
                  </Select>
                ) : (
                  <p className="text-xs text-muted-foreground">{t("noNextStatus")}</p>
                )}
              </section>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  );
}
