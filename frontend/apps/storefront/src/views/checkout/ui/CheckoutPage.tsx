"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { AlertTriangle, Loader2, Clock } from "lucide-react";
import { Button } from "@radolfa/shared/ui/button";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { useCartQuery } from "@/features/cart";
import { useCheckout, CheckoutStepper, DeliveryStep, ReviewStep, PaymentStep } from "@/features/checkout";
import { initiatePayment } from "@/features/payment";
import { useCancelOrder } from "@/entities/order";
import { getErrorMessage } from "@radolfa/shared/lib";

export function CheckoutPage() {
  const t = useTranslations("checkout");
  const router = useRouter();

  const { data: cart, isLoading: loadingCart } = useCartQuery();
  const wizard = useCheckout();

  const [redirecting, setRedirecting] = useState(false);
  const cancelOrderMutation = useCancelOrder();

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
    router.replace("/search");
    return null;
  }

  /* ── Pending order — user went back from payment page ──────── */
  if (cart?.pendingOrderId) {
    const pendingOrderId = cart.pendingOrderId;
    return (
      <div className="max-w-lg mx-auto px-4 py-16 space-y-6 text-center">
        <div className="flex flex-col items-center gap-4">
          <div className="rounded-full bg-amber-100 p-4">
            <Clock className="h-8 w-8 text-amber-600" />
          </div>
          <div>
            <h1 className="text-xl font-semibold mb-1">You have an order awaiting payment</h1>
            <p className="text-sm text-muted-foreground">
              Order #{pendingOrderId} was placed but not yet paid. You can resume payment or cancel it to start over.
            </p>
          </div>
        </div>

        <div className="flex flex-col gap-3">
          <Button
            className="w-full h-12 text-base font-semibold"
            disabled={redirecting}
            onClick={async () => {
              setRedirecting(true);
              try {
                const { redirectUrl } = await initiatePayment(pendingOrderId);
                window.location.href = redirectUrl;
              } catch (err) {
                setRedirecting(false);
                toast.error(getErrorMessage(err));
              }
            }}
          >
            {redirecting ? (
              <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Redirecting…</>
            ) : (
              "Resume Payment"
            )}
          </Button>

          <Button
            variant="outline"
            className="w-full h-12 text-base"
            disabled={cancelOrderMutation.isPending}
            onClick={() => {
              cancelOrderMutation.mutate(
                { orderId: pendingOrderId, reason: "Cancelled by customer" },
                { onError: (err) => toast.error(getErrorMessage(err)) }
              );
            }}
          >
            {cancelOrderMutation.isPending ? (
              <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Cancelling…</>
            ) : (
              "Cancel Order & Start Over"
            )}
          </Button>
        </div>
      </div>
    );
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

  return (
    <>
      <CheckoutStepper step={wizard.step} onStepClick={wizard.goStep} />
      <div className="max-w-2xl mx-auto px-4 py-10 space-y-6">
        <h1 className="text-2xl font-bold">{t("title")}</h1>

        {/* Out-of-stock warning */}
        {wizard.hasOutOfStockItems && (
          <div className="flex items-start gap-3 rounded-xl border border-destructive/30 bg-destructive/5 p-4">
            <AlertTriangle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
            <p className="text-sm text-destructive">{t("outOfStockWarning")}</p>
          </div>
        )}

        {wizard.step === "delivery" && <DeliveryStep checkout={wizard} />}

        {wizard.step === "review" && <ReviewStep checkout={wizard} />}

        {wizard.step === "payment" && <PaymentStep checkout={wizard} />}

        {/* Done — interim placeholder; real ConfirmationStep ships in Phase 14 */}
        {wizard.step === "done" && (
          <div className="rounded-xl border bg-card shadow-sm p-5 space-y-2 text-center">
            <h2 className="font-semibold">{t("successTitle")}</h2>
            {wizard.checkoutResult && (
              <p className="text-sm text-muted-foreground">
                {t("orderNumber", { id: wizard.checkoutResult.orderId })}
              </p>
            )}
          </div>
        )}
      </div>
    </>
  );
}
