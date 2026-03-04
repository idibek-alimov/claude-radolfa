import { apiClient } from "@/shared/api";
import type { Cart } from "@/entities/cart/model/types";

export async function getCart(): Promise<Cart> {
  const { data } = await apiClient.get<Cart>("/api/v1/cart");
  return data;
}

export async function addToCart(skuId: number, quantity: number): Promise<Cart> {
  const { data } = await apiClient.post<Cart>("/api/v1/cart/items", { skuId, quantity });
  return data;
}

export async function updateCartItem(skuId: number, quantity: number): Promise<Cart> {
  const { data } = await apiClient.put<Cart>(`/api/v1/cart/items/${skuId}`, { quantity });
  return data;
}

export async function removeCartItem(skuId: number): Promise<Cart> {
  const { data } = await apiClient.delete<Cart>(`/api/v1/cart/items/${skuId}`);
  return data;
}

export async function clearCart(): Promise<void> {
  await apiClient.delete("/api/v1/cart");
}
