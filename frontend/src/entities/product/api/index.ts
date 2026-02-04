import { apiClient } from "@/shared/api";
import type { Product } from "@/entities/product";

/**
 * Fetch a single product by its stable ERP identifier.
 * Endpoint does not exist yet — will be wired in a later backend phase.
 */
export async function fetchProductByErpId(erpId: string): Promise<Product> {
  const { data } = await apiClient.get<Product>(
    `/api/v1/products/${erpId}`
  );
  return data;
}
