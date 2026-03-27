import apiClient from "@/shared/api/axios";

export interface PaymentInitResponse {
  paymentId: number;
  redirectUrl: string;
}

export interface PaymentStatusResponse {
  paymentId: number;
  status: "PENDING" | "COMPLETED" | "REFUNDED";
  provider: string;
  amount: number;
}

export async function initiatePayment(orderId: number): Promise<PaymentInitResponse> {
  const response = await apiClient.post<PaymentInitResponse>(
    `/api/v1/payments/initiate/${orderId}`
  );
  return response.data;
}

export async function getPaymentStatus(orderId: number): Promise<PaymentStatusResponse> {
  const response = await apiClient.get<PaymentStatusResponse>(`/api/v1/payments/${orderId}`);
  return response.data;
}
