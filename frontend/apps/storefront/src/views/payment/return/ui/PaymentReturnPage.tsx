"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import Link from "next/link";
import {
  Clock,
  XCircle,
  RefreshCw,
  Loader2,
  ChevronRight,
} from "lucide-react";
import { Button } from "@radolfa/shared/ui/button";
import { getPaymentStatus } from "@/features/payment";
import { useAuth } from "@radolfa/shared/auth";
import { useMyOrderDetail } from "@/features/order-detail";
import { ConfirmationStep } from "@/features/checkout";

const POLL_INTERVAL_MS = 2000;
const POLL_TIMEOUT_MS = 30000;

type PollState = "polling" | "completed" | "pending_timeout" | "refunded" | "error";

export function PaymentReturnPage() {
  const t = useTranslations("payment");
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const { refreshUser } = useAuth();

  const orderIdRaw = searchParams.get("orderId");
  const orderId = orderIdRaw ? parseInt(orderIdRaw, 10) : null;

  const [timedOut, setTimedOut] = useState(false);
  const [pollState, setPollState] = useState<PollState>("polling");
  const [manualRefresh, setManualRefresh] = useState(0);

  /* ── 30-second timeout ───────────────────────────────────────── */
  useEffect(() => {
    if (!orderId) return;
    const timer = setTimeout(() => setTimedOut(true), POLL_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [orderId]);

  const { data, isError } = useQuery({
    queryKey: ["payment-status", orderId, manualRefresh],
    queryFn: () => getPaymentStatus(orderId!),
    enabled: !!orderId && pollState === "polling" && !timedOut,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === "COMPLETED" || status === "REFUNDED") return false;
      if (timedOut) return false;
      return POLL_INTERVAL_MS;
    },
  });

  const { data: orderDetail } = useMyOrderDetail(
    pollState === "completed" && orderId ? String(orderId) : ""
  );

  /* ── React to status changes ─────────────────────────────────── */
  useEffect(() => {
    if (!data) return;

    if (data.status === "COMPLETED" && pollState === "polling") {
      setPollState("completed");
      queryClient.invalidateQueries({ queryKey: ["cart"] });
      queryClient.invalidateQueries({ queryKey: ["my-orders"] });
      refreshUser();
    } else if (data.status === "REFUNDED" && pollState === "polling") {
      setPollState("refunded");
    }
  }, [data, pollState, queryClient, refreshUser]);

  useEffect(() => {
    if (isError) setPollState("error");
  }, [isError]);

  useEffect(() => {
    if (timedOut && pollState === "polling") setPollState("pending_timeout");
  }, [timedOut, pollState]);

  /* ── Missing orderId ─────────────────────────────────────────── */
  if (!orderId) {
    return (
      <div className="max-w-[1240px] mx-auto px-4 md:px-6 py-10 md:py-16">
        <div className="bg-white rounded-2xl border border-ink/8 p-8 md:p-10 text-center max-w-lg mx-auto">
          <XCircle className="h-16 w-16 text-destructive mx-auto mb-6" />
          <h1 className="text-2xl font-bold mb-2">{t("errorTitle")}</h1>
          <p className="text-muted-foreground mb-8">{t("errorDesc")}</p>
          <Link href="/search">
            <Button variant="outline">{t("browsProducts")}</Button>
          </Link>
        </div>
      </div>
    );
  }

  /* ── Polling ─────────────────────────────────────────────────── */
  if (pollState === "polling") {
    return (
      <div className="max-w-[1240px] mx-auto px-4 md:px-6 py-10 md:py-16">
        <div className="bg-white rounded-2xl border border-ink/8 p-8 md:p-10 text-center max-w-lg mx-auto">
          <Loader2 className="h-16 w-16 text-mag mx-auto mb-6 animate-spin" />
          <h1 className="text-xl font-semibold mb-2">{t("pollingTitle")}</h1>
          <p className="text-sm text-muted-foreground">{t("pollingDesc")}</p>
        </div>
      </div>
    );
  }

  /* ── Completed ───────────────────────────────────────────────── */
  if (pollState === "completed") {
    return (
      <div className="max-w-[1240px] mx-auto px-4 md:px-6 py-10 md:py-16">
        <ConfirmationStep
          orderId={orderId}
          total={data?.amount ?? 0}
          paymentMethod="CARD"
          deliveryType={orderDetail?.deliveryType ?? null}
        />
      </div>
    );
  }

  /* ── Pending timeout ─────────────────────────────────────────── */
  if (pollState === "pending_timeout") {
    return (
      <div className="max-w-[1240px] mx-auto px-4 md:px-6 py-10 md:py-16">
        <div className="bg-white rounded-2xl border border-ink/8 p-8 md:p-10 text-center max-w-lg mx-auto">
          <Clock className="h-16 w-16 text-gold mx-auto mb-6" />
          <h1 className="text-2xl font-bold mb-2">{t("processingTitle")}</h1>
          <p className="text-muted-foreground mb-8">{t("processingDesc")}</p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Button
              variant="outline"
              className="gap-2"
              onClick={() => {
                setPollState("polling");
                setTimedOut(false);
                setManualRefresh((n) => n + 1);
                setTimeout(() => setTimedOut(true), POLL_TIMEOUT_MS);
              }}
            >
              <RefreshCw className="h-4 w-4" />
              {t("retryCheck")}
            </Button>
            <Link href="/profile/orders">
              <Button className="gap-2 w-full sm:w-auto">
                {t("viewOrders")}
                <ChevronRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  /* ── Refunded or error ───────────────────────────────────────── */
  return (
    <div className="max-w-[1240px] mx-auto px-4 md:px-6 py-10 md:py-16">
      <div className="bg-white rounded-2xl border border-ink/8 p-8 md:p-10 text-center max-w-lg mx-auto">
        <XCircle className="h-16 w-16 text-destructive mx-auto mb-6" />
        <h1 className="text-2xl font-bold mb-2">
          {pollState === "refunded" ? t("refundedTitle") : t("failureTitle")}
        </h1>
        <p className="text-muted-foreground mb-8">
          {pollState === "refunded" ? t("refundedDesc") : t("failureDesc")}
        </p>
        <Link href="/profile/orders">
          <Button variant="outline">{t("viewOrders")}</Button>
        </Link>
      </div>
    </div>
  );
}
