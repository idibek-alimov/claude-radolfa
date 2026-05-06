import apiClient from "@/shared/api/axios";
import type { DeliveryType } from "./model/types";

export interface CheckoutRequest {
  loyaltyPointsToRedeem: number;
  notes?: string;
  deliveryType: DeliveryType;
  address?: string;
  preferredTimeWindow?: string;
  pickpointId?: number;
}

export interface CheckoutResponse {
  orderId: number;
  status: string;
  subtotal: number;
  tierDiscount: number;
  pointsDiscount: number;
  total: number;
}

export async function checkout(payload: CheckoutRequest): Promise<CheckoutResponse> {
  const response = await apiClient.post<CheckoutResponse>("/api/v1/orders/checkout", payload);
  return response.data;
}
