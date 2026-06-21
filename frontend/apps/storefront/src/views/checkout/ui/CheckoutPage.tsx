"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { AlertTriangle, Loader2, Clock } from "lucide-react";
import { Button } from "@radolfa/shared/ui/button";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { useCartQuery } from "@/features/cart";
import {
  useCheckout,
  CheckoutStepper,
  DeliveryStep,
  ReviewStep,
  PaymentStep,
  ConfirmationStep,
  CheckoutSummary,
  MobileCheckoutBar,
} from "@/features/checkout";
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
  /* Skipped on "done": a successful checkout finalizes (empties) the cart,
   * and the confirmation step must still render, not bounce to /search. */
  if ((!cart || cart.items.length === 0) && wizard.step !== "done") {
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
      <section className="max-w-[1240px] mx-auto px-4 md:px-6 py-6 md:py-8 grid grid-cols-1 lg:grid-cols-3 gap-5 lg:gap-7 items-start">
        <div className={wizard.step === "done" ? "lg:col-span-3" : "lg:col-span-2"}>
          <div className={`flex flex-col gap-5 ${wizard.step === "done" ? "" : "pb-28 lg:pb-0"}`}>
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

            {wizard.step === "done" && wizard.checkoutResult && (
              <ConfirmationStep
                orderId={wizard.checkoutResult.orderId}
                total={wizard.checkoutResult.total}
                paymentMethod={wizard.checkoutResult.paymentMethod}
                deliveryType={wizard.deliveryType}
              />
            )}
          </div>
        </div>

        {wizard.step !== "done" && (
          <aside className="hidden lg:flex flex-col gap-4 sticky top-28">
            <CheckoutSummary checkout={wizard} />
          </aside>
        )}
      </section>
      <MobileCheckoutBar checkout={wizard} />
    </>
  );
}
