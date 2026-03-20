"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import Link from "next/link";
import {
  ShoppingBag,
  AlertTriangle,
  Star,
  CheckCircle2,
  ChevronRight,
} from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import { useCartQuery } from "@/features/cart";
import { useAuth } from "@/features/auth";
import { checkout } from "@/features/checkout";
import type { CheckoutResponse } from "@/features/checkout";
import { getErrorMessage } from "@/shared/lib";

export function CheckoutPage() {
  const t = useTranslations("checkout");
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const { data: cart, isLoading: loadingCart } = useCartQuery();

  const [pointsToRedeem, setPointsToRedeem] = useState(0);
  const [notes, setNotes] = useState("");
  const [successData, setSuccessData] = useState<CheckoutResponse | null>(null);

  const availablePoints = user?.loyalty?.points ?? 0;
  const hasOutOfStockItems = cart?.items.some((i) => !i.inStock) ?? false;

  const checkoutMutation = useMutation({
    mutationFn: () =>
      checkout({ loyaltyPointsToRedeem: pointsToRedeem, notes: notes || undefined }),
    onSuccess: (data) => {
      setSuccessData(data);
      queryClient.invalidateQueries({ queryKey: ["cart"] });
      queryClient.invalidateQueries({ queryKey: ["my-orders"] });
    },
    onError: (err: unknown) => {
      toast.error(getErrorMessage(err));
    },
  });

  /* ── Loading ─────────────────────────────────────────────────── */
  if (loadingCart) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12 space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-20 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  /* ── Empty cart ──────────────────────────────────────────────── */
  if (!cart || cart.items.length === 0) {
    router.replace("/products");
    return null;
  }

  /* ── Success screen ──────────────────────────────────────────── */
  if (successData) {
    return (
      <div className="max-w-lg mx-auto px-4 py-16 text-center">
        <div className="flex justify-center mb-6">
          <CheckCircle2 className="h-16 w-16 text-green-500" />
        </div>
        <h1 className="text-2xl font-bold mb-2">{t("successTitle")}</h1>
        <p className="text-muted-foreground mb-1">
          {t("orderNumber", { id: successData.orderId })}
        </p>
        <p className="text-muted-foreground mb-8">
          {t("total")}: {successData.total.toFixed(2)} TJS
        </p>
        <Link href="/profile?tab=orders">
          <Button className="gap-2">
            {t("viewOrders")}
            <ChevronRight className="h-4 w-4" />
          </Button>
        </Link>
      </div>
    );
  }

  /* ── Points monetary value (1 point = 0.01 TJS) ─────────────── */
  const pointsValue = pointsToRedeem * 0.01;
  const estimatedTotal = Math.max(0, cart.totalAmount - pointsValue);

  return (
    <div className="max-w-2xl mx-auto px-4 py-10 space-y-6">
      <h1 className="text-2xl font-bold">{t("title")}</h1>

      {/* Out-of-stock warning */}
      {hasOutOfStockItems && (
        <div className="flex items-start gap-3 rounded-xl border border-destructive/30 bg-destructive/5 p-4">
          <AlertTriangle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
          <p className="text-sm text-destructive">{t("outOfStockWarning")}</p>
        </div>
      )}

      {/* Order summary */}
      <div className="rounded-xl border bg-card shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b bg-muted/30">
          <h2 className="font-semibold flex items-center gap-2">
            <ShoppingBag className="h-4 w-4" />
            {t("orderSummary")}
          </h2>
        </div>
        <ul className="divide-y">
          {cart.items.map((item) => (
            <li
              key={item.skuId}
              className={`flex items-center gap-4 px-5 py-3 ${
                !item.inStock ? "opacity-60" : ""
              }`}
            >
              {item.imageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={item.imageUrl}
                  alt={item.productName}
                  className="h-12 w-12 rounded-lg object-cover shrink-0"
                />
              ) : (
                <div className="h-12 w-12 rounded-lg bg-muted flex items-center justify-center shrink-0">
                  <ShoppingBag className="h-5 w-5 text-muted-foreground" />
                </div>
              )}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{item.productName}</p>
                <p className="text-xs text-muted-foreground">
                  {item.colorName} · {item.sizeLabel}
                </p>
                {!item.inStock && (
                  <p className="text-xs text-destructive font-medium">{t("outOfStock")}</p>
                )}
              </div>
              <div className="text-right shrink-0">
                <p className="text-sm font-semibold">{item.lineTotal.toFixed(2)} TJS</p>
                <p className="text-xs text-muted-foreground">× {item.quantity}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>

      {/* Loyalty points */}
      {availablePoints > 0 && (
        <div className="rounded-xl border bg-card shadow-sm p-5">
          <h2 className="font-semibold flex items-center gap-2 mb-3">
            <Star className="h-4 w-4 fill-amber-500 text-amber-500" />
            {t("loyaltyPoints")}
          </h2>
          <p className="text-sm text-muted-foreground mb-3">
            {t("availablePoints", { count: availablePoints })}
          </p>
          <div className="flex items-center gap-3">
            <Input
              type="number"
              min={0}
              max={availablePoints}
              value={pointsToRedeem === 0 ? "" : pointsToRedeem}
              onChange={(e) => {
                const val = Math.min(
                  Math.max(0, parseInt(e.target.value) || 0),
                  availablePoints
                );
                setPointsToRedeem(val);
              }}
              placeholder="0"
              className="w-36 h-9 text-sm"
            />
            <span className="text-sm text-muted-foreground">
              {pointsToRedeem > 0 && `= ${pointsValue.toFixed(2)} TJS`}
            </span>
          </div>
        </div>
      )}

      {/* Notes */}
      <div className="rounded-xl border bg-card shadow-sm p-5">
        <h2 className="font-semibold mb-3">{t("notes")}</h2>
        <textarea
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder={t("notesPlaceholder")}
          rows={3}
          className="w-full rounded-lg border bg-background px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-ring"
        />
      </div>

      {/* Total breakdown */}
      <div className="rounded-xl border bg-card shadow-sm p-5 space-y-2">
        <h2 className="font-semibold mb-3">{t("totalBreakdown")}</h2>
        <div className="flex justify-between text-sm">
          <span className="text-muted-foreground">{t("subtotal")}</span>
          <span>{cart.totalAmount.toFixed(2)} TJS</span>
        </div>
        {pointsToRedeem > 0 && (
          <div className="flex justify-between text-sm text-amber-700">
            <span>{t("pointsDiscount")}</span>
            <span>−{pointsValue.toFixed(2)} TJS</span>
          </div>
        )}
        <div className="h-px bg-border my-1" />
        <div className="flex justify-between font-semibold">
          <span>{t("total")}</span>
          <span>{estimatedTotal.toFixed(2)} TJS</span>
        </div>
      </div>

      {/* Place Order */}
      <Button
        className="w-full h-12 text-base font-semibold"
        disabled={hasOutOfStockItems || checkoutMutation.isPending}
        onClick={() => checkoutMutation.mutate()}
      >
        {checkoutMutation.isPending ? t("placing") : t("placeOrder")}
      </Button>
    </div>
  );
}
