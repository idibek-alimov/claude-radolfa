import apiClient from "@radolfa/shared/api/axios";
import type { CheckoutOptions, DeliveryType, PaymentMethod } from "./model/types";

export interface CheckoutRequest {
  loyaltyPointsToRedeem: number;
  notes?: string;
  deliveryType: DeliveryType;
  address?: string;
  preferredTimeWindow?: string;
  pickpointId?: number;
  paymentMethod?: PaymentMethod;
}

export interface CheckoutResponse {
  orderId: number;
  status: string;
  subtotal: number;
  tierDiscount: number;
  pointsDiscount: number;
  total: number;
  paymentMethod: PaymentMethod;
  handlingFee: number;
}

export async function checkout(payload: CheckoutRequest): Promise<CheckoutResponse> {
  const response = await apiClient.post<CheckoutResponse>("/api/v1/orders/checkout", payload);
  return response.data;
}

export async function fetchCheckoutOptions(): Promise<CheckoutOptions> {
  const response = await apiClient.get<CheckoutOptions>("/api/v1/checkout/options");
  return response.data;
}
