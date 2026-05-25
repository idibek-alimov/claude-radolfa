// TODO(payments): Fake "Bank of Radolfa" page — replace with real PSP-hosted checkout
// (e.g. Tinkoff, CloudPayments) when payment gateway integration is ready.
// Remove this file and StubPaymentCallbackController.java at the same time.
"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { CreditCard, Loader2, Lock, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { getPaymentStatus } from "@/features/payment/api";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Skeleton } from "@/shared/ui/skeleton";

function formatCardNumber(raw: string): string {
  const digits = raw.replace(/\D/g, "").slice(0, 16);
  return digits.replace(/(.{4})/g, "$1 ").trimEnd();
}

function formatExpiry(raw: string): string {
  const digits = raw.replace(/\D/g, "").slice(0, 4);
  if (digits.length <= 2) return digits;
  return digits.slice(0, 2) + "/" + digits.slice(2);
}

function isExpiryValid(value: string): boolean {
  if (value.length !== 5) return false;
  const mm = parseInt(value.slice(0, 2), 10);
  return mm >= 1 && mm <= 12;
}

function isFormValid(card: string, name: string, expiry: string, cvv: string): boolean {
  const digits = card.replace(/\s/g, "");
  return (
    digits.length === 16 &&
    name.trim().length > 0 &&
    isExpiryValid(expiry) &&
    cvv.length === 3
  );
}

export function BankPaymentPage() {
  const router = useRouter();
  const params = useSearchParams();

  const tx = params.get("tx");
  const orderIdRaw = params.get("orderId");
  const orderId = orderIdRaw ? parseInt(orderIdRaw, 10) : null;

  useEffect(() => {
    if (!tx || !orderId) {
      toast.error("Invalid payment session.");
      router.replace("/cart");
    }
  }, [tx, orderId, router]);

  const { data: payment, isLoading: loadingPayment } = useQuery({
    queryKey: ["payment-status", orderId],
    queryFn: () => getPaymentStatus(orderId!),
    enabled: !!orderId,
    refetchOnWindowFocus: false,
  });

  const [cardNumber, setCardNumber] = useState("");
  const [cardName, setCardName] = useState("");
  const [expiry, setExpiry] = useState("");
  const [cvv, setCvv] = useState("");
  const [processing, setProcessing] = useState(false);

  const canSubmit = isFormValid(cardNumber, cardName, expiry, cvv);

  function handlePay() {
    if (!canSubmit || !tx || !orderId) return;
    setProcessing(true);
    setTimeout(() => {
      window.location.href = `/api/v1/payments/stub/confirm?tx=${tx}&orderId=${orderId}`;
    }, 3000);
  }

  if (!tx || !orderId) return null;

  return (
    <div className="min-h-screen bg-muted/40 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-card rounded-2xl shadow-xl overflow-hidden border">

        {/* Bank header */}
        <div className="bg-primary text-primary-foreground px-6 py-5">
          <div className="flex items-center gap-3">
            <div className="bg-primary-foreground/20 rounded-full p-2">
              <CreditCard className="h-5 w-5" />
            </div>
            <div>
              <p className="text-lg font-bold tracking-tight">Bank of Radolfa</p>
              <p className="text-xs text-primary-foreground/70">Secure Payment Gateway</p>
            </div>
          </div>
        </div>

        <div className="p-6 space-y-5">
          {/* Order summary */}
          <div className="bg-muted/30 rounded-xl p-5 space-y-1">
            <p className="text-xs text-muted-foreground uppercase tracking-wide">Order total</p>
            {loadingPayment ? (
              <>
                <Skeleton className="h-8 w-32" />
                <Skeleton className="h-4 w-24 mt-1" />
              </>
            ) : (
              <>
                <p className="text-2xl font-bold tabular-nums">
                  {payment?.amount?.toLocaleString("ru-TJ", { minimumFractionDigits: 2 })} TJS
                </p>
                <p className="text-xs text-muted-foreground">Order #{orderId}</p>
              </>
            )}
          </div>

          {/* Card form */}
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="card-number">Card number</Label>
              <Input
                id="card-number"
                placeholder="0000 0000 0000 0000"
                value={cardNumber}
                onChange={(e) => setCardNumber(formatCardNumber(e.target.value))}
                maxLength={19}
                disabled={processing}
                autoComplete="cc-number"
                inputMode="numeric"
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="card-name">Cardholder name</Label>
              <Input
                id="card-name"
                placeholder="JOHN DOE"
                value={cardName}
                onChange={(e) => setCardName(e.target.value)}
                onBlur={(e) => setCardName(e.target.value.toUpperCase())}
                disabled={processing}
                autoComplete="cc-name"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="expiry">Expiry (MM/YY)</Label>
                <Input
                  id="expiry"
                  placeholder="MM/YY"
                  value={expiry}
                  onChange={(e) => {
                    const formatted = formatExpiry(e.target.value);
                    setExpiry(formatted);
                  }}
                  maxLength={5}
                  disabled={processing}
                  autoComplete="cc-exp"
                  inputMode="numeric"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="cvv">CVV</Label>
                <Input
                  id="cvv"
                  placeholder="•••"
                  type="password"
                  value={cvv}
                  onChange={(e) => setCvv(e.target.value.replace(/\D/g, "").slice(0, 3))}
                  maxLength={3}
                  disabled={processing}
                  autoComplete="cc-csc"
                  inputMode="numeric"
                />
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="space-y-3 pt-1">
            <Button
              className="w-full"
              onClick={handlePay}
              disabled={!canSubmit || processing}
            >
              {processing ? (
                <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Processing…</>
              ) : (
                <>
                  Pay{" "}
                  {payment?.amount?.toLocaleString("ru-TJ", { minimumFractionDigits: 2 })}{" "}
                  TJS
                </>
              )}
            </Button>

            <Button
              variant="outline"
              className="w-full"
              onClick={() => router.push("/checkout")}
              disabled={processing}
            >
              Cancel
            </Button>
          </div>

          {/* Trust badges */}
          <div className="flex items-center justify-center gap-4 pt-2 text-muted-foreground">
            <div className="flex items-center gap-1 text-xs">
              <Lock className="h-3 w-3" />
              <span>256-bit SSL</span>
            </div>
            <div className="flex items-center gap-1 text-xs">
              <ShieldCheck className="h-3 w-3" />
              <span>PCI DSS</span>
            </div>
          </div>
        </div>
      </div>

      {/* Full-page processing overlay */}
      {processing && (
        <div className="fixed inset-0 bg-background/80 backdrop-blur-sm flex flex-col items-center justify-center gap-4 z-50">
          <Loader2 className="h-12 w-12 animate-spin text-primary" />
          <div className="text-center space-y-1">
            <p className="text-base font-semibold">Contacting your bank…</p>
            <p className="text-sm text-muted-foreground">Please do not close this window</p>
          </div>
        </div>
      )}
    </div>
  );
}
