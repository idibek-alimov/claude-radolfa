"use client";

import Image from "next/image";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { X, MapPin, Home, Package } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/shared/ui/sheet";
import { Button } from "@/shared/ui/button";
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

const NEXT_STATUS: Record<OrderStatus, OrderStatus | null> = {
  PENDING:   "PAID",
  PAID:      "SHIPPED",
  SHIPPED:   "DELIVERED",
  DELIVERED: null,
  CANCELLED: null,
};

interface Props {
  orderId: number | null;
  onOpenChange: (open: boolean) => void;
}

export function OrderDetailDrawer({ orderId, onOpenChange }: Props) {
  const t = useTranslations("manage.orders.drawer");
  const tStatus = useTranslations("manage.orders.status");

  const { data: order, isLoading } = useAdminOrder(orderId);
  const updateStatus = useUpdateOrderStatus();

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

  const nextStatus = order ? NEXT_STATUS[order.status] : null;

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

              {/* Status transition */}
              <section>
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">
                  {t("statusLabel")}
                </p>
                {nextStatus ? (
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
