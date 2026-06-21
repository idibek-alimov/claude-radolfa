"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useCartQuery } from "@/features/cart";
import { initiatePayment } from "@/features/payment";
import { checkout, type CheckoutResponse } from "../api";
import { useCheckoutOptions } from "./useCheckoutOptions";
import {
  CHECKOUT_STEPS,
  type CheckoutStep,
  type DeliveryType,
  type PaymentMethod,
  type TimeWindowCode,
} from "../model/types";

export function useCheckout() {
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: cart } = useCartQuery();
  const { data: options } = useCheckoutOptions();
  const codHandlingFee = options?.codHandlingFee ?? 0;

  const [step, setStep] = useState<CheckoutStep>("delivery");
  const [deliveryType, setDeliveryType] = useState<DeliveryType>("HOME");
  const [address, setAddress] = useState("");
  const [timeWindow, setTimeWindow] = useState<TimeWindowCode>("MORNING");
  const [pickpointId, setPickpointId] = useState<number | null>(null);
  const [notes, setNotes] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("CARD");
  const [pointsToRedeem, setPointsToRedeem] = useState(0);
  const [checkoutResult, setCheckoutResult] = useState<CheckoutResponse | null>(null);

  const addressMissing = deliveryType === "HOME" && address.trim().length === 0;
  const pickpointMissing = deliveryType === "PICKPOINT" && pickpointId === null;
  const deliveryInvalid = addressMissing || pickpointMissing;
  const hasOutOfStockItems = cart?.items.some((i) => !i.inStock) ?? false;

  function goStep(target: CheckoutStep) {
    setStep(target);
  }

  function next() {
    if (step === "delivery" && deliveryInvalid) return;
    const idx = CHECKOUT_STEPS.indexOf(step);
    const target = CHECKOUT_STEPS[idx + 1];
    if (target) setStep(target);
  }

  function back() {
    const idx = CHECKOUT_STEPS.indexOf(step);
    const target = CHECKOUT_STEPS[idx - 1];
    if (target) setStep(target);
  }

  const placeOrderMutation = useMutation({
    mutationFn: () =>
      checkout({
        loyaltyPointsToRedeem: pointsToRedeem,
        notes: notes || undefined,
        deliveryType,
        address: deliveryType === "HOME" ? address.trim() : undefined,
        preferredTimeWindow: deliveryType === "HOME" ? timeWindow : undefined,
        pickpointId: deliveryType === "PICKPOINT" ? (pickpointId ?? undefined) : undefined,
        paymentMethod,
      }),
    onSuccess: async (response) => {
      queryClient.invalidateQueries({ queryKey: ["cart"] });
      queryClient.invalidateQueries({ queryKey: ["my-orders"] });
      setCheckoutResult(response);

      if (response.paymentMethod === "COD") {
        goStep("done");
        return;
      }

      try {
        const { redirectUrl } = await initiatePayment(response.orderId);
        window.location.href = redirectUrl;
      } catch {
        router.push("/profile/orders");
      }
    },
  });

  return {
    step,
    goStep,
    next,
    back,

    deliveryType,
    setDeliveryType,
    address,
    setAddress,
    timeWindow,
    setTimeWindow,
    pickpointId,
    setPickpointId,
    notes,
    setNotes,
    paymentMethod,
    setPaymentMethod,
    pointsToRedeem,
    setPointsToRedeem,

    cart,
    codHandlingFee,
    hasOutOfStockItems,
    deliveryInvalid,
    addressMissing,
    pickpointMissing,

    placeOrder: placeOrderMutation.mutate,
    isPlacing: placeOrderMutation.isPending,
    placeOrderError: placeOrderMutation.error,
    checkoutResult,
  };
}
