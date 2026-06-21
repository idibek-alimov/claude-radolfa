// Synced from backend — do not edit manually
export type DeliveryType = "HOME" | "PICKPOINT";
export type TimeWindowCode = "MORNING" | "AFTERNOON" | "EVENING";

export const TIME_WINDOW_CODES: ReadonlyArray<TimeWindowCode> = [
  "MORNING",
  "AFTERNOON",
  "EVENING",
] as const;

export type PaymentMethod = "CARD" | "COD";

export interface CheckoutOptions {
  codHandlingFee: number;
  paymentMethods: PaymentMethod[];
}

export type CheckoutStep = "delivery" | "review" | "payment" | "done";

export const CHECKOUT_STEPS: ReadonlyArray<CheckoutStep> = [
  "delivery",
  "review",
  "payment",
  "done",
] as const;
