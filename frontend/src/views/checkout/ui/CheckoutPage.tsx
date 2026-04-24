"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import type { ApplyCouponResponse } from "@/entities/cart";
import {
  ShoppingBag,
  AlertTriangle,
  Star,
  Loader2,
  Tag,
  X,
} from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import { useCartQuery, useApplyCoupon, useRemoveCoupon } from "@/features/cart";
import { useAuth } from "@/features/auth";
import { checkout } from "@/features/checkout";
import { initiatePayment } from "@/features/payment";
import { getErrorMessage, isCouponsEnabled } from "@/shared/lib";

export function CheckoutPage() {
  const t = useTranslations("checkout");
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const { data: cart, isLoading: loadingCart } = useCartQuery();

  const [pointsToRedeem, setPointsToRedeem] = useState(0);
  const [notes, setNotes] = useState("");
  const [redirecting, setRedirecting] = useState(false);
  const [couponInput, setCouponInput] = useState("");
  const [couponError, setCouponError] = useState<string | null>(null);
  const [affectedCount, setAffectedCount] = useState<number | null>(null);

  const applyCouponMutation = useApplyCoupon();
  const removeCouponMutation = useRemoveCoupon();

  function applyCoupon() {
    const code = couponInput.trim();
    if (!code) return;
    setCouponError(null);
    applyCouponMutation.mutate(code, {
      onSuccess: (data: ApplyCouponResponse) => {
        if (data.valid) {
          setCouponInput("");
          setAffectedCount(data.affectedSkus.length);
          toast.success(t("coupon.applied"));
        } else {
          const key = `coupon.${data.invalidReason}` as Parameters<typeof t>[0];
          setCouponError(t(key));
        }
      },
      onError: (err: unknown) => setCouponError(getErrorMessage(err)),
    });
  }

  const availablePoints = user?.loyalty?.points ?? 0;
  const hasOutOfStockItems = cart?.items.some((i) => !i.inStock) ?? false;

  const checkoutMutation = useMutation({
    mutationFn: () =>
      checkout({ loyaltyPointsToRedeem: pointsToRedeem, notes: notes || undefined }),
    onSuccess: async (data) => {
      queryClient.invalidateQueries({ queryKey: ["cart"] });
      queryClient.invalidateQueries({ queryKey: ["my-orders"] });
      setRedirecting(true);
      try {
        const { redirectUrl } = await initiatePayment(data.orderId);
        window.location.href = redirectUrl;
      } catch {
        // Payment initiation failed — order was still placed; send user to orders
        router.push("/profile?tab=orders");
      }
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

  /* ── Redirecting to payment provider ────────────────────────── */
  if (redirecting) {
    return (
      <div className="max-w-lg mx-auto px-4 py-16 text-center">
        <Loader2 className="h-16 w-16 text-primary mx-auto mb-6 animate-spin" />
        <h1 className="text-xl font-semibold mb-2">{t("redirecting")}</h1>
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

      {/* Coupon code */}
      {isCouponsEnabled && (
        <div className="rounded-xl border bg-card shadow-sm p-5">
          <h2 className="font-semibold flex items-center gap-2 mb-3">
            <Tag className="h-4 w-4" />
            {t("coupon.label")}
          </h2>

          {cart.couponCode ? (
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 text-primary text-sm font-mono font-medium px-3 py-1">
                  <Tag className="h-3.5 w-3.5" />
                  {cart.couponCode}
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setAffectedCount(null);
                    removeCouponMutation.mutate(undefined, {
                      onError: (err) => toast.error(getErrorMessage(err)),
                    });
                  }}
                  disabled={removeCouponMutation.isPending}
                >
                  <X className="h-4 w-4" />
                  {t("coupon.remove")}
                </Button>
              </div>
              {affectedCount !== null && (
                <p className="text-xs text-muted-foreground">
                  {t("coupon.affectedItems", { count: affectedCount })}
                </p>
              )}
            </div>
          ) : (
            <div className="flex gap-2">
              <Input
                placeholder={t("coupon.placeholder")}
                value={couponInput}
                onChange={(e) => {
                  setCouponInput(e.target.value.toUpperCase());
                  setCouponError(null);
                }}
                className="font-mono tracking-widest max-w-xs h-9 text-sm"
                maxLength={32}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    applyCoupon();
                  }
                }}
              />
              <Button
                variant="outline"
                size="sm"
                disabled={!couponInput.trim() || applyCouponMutation.isPending}
                onClick={applyCoupon}
              >
                {applyCouponMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  t("coupon.apply")
                )}
              </Button>
            </div>
          )}

          {couponError && (
            <p className="text-xs text-destructive mt-2">{couponError}</p>
          )}
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
