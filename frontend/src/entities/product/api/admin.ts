import { apiClient } from "@/shared/api";

export interface SkuDefinition {
  sizeLabel: string;
  price: number;
  stockQuantity: number;
}

export interface CreateProductRequest {
  name: string;
  categoryId: number;
  colorId: number;
  skus: SkuDefinition[];
}

export interface CreateProductResponse {
  productBaseId: number;
  variantId: number;
  slug: string;
}

export interface UpdateStockPayload {
  quantity?: number;  // absolute set
  delta?: number;     // relative adjustment (signed)
}

export interface SkuPriceResponse {
  skuId: number;
  price: number;
}

export interface SkuStockResponse {
  skuId: number;
  stockQuantity: number;
}

/** Create a new product with color and SKU definitions (MANAGER+). */
export async function createProduct(
  payload: CreateProductRequest
): Promise<CreateProductResponse> {
  const { data } = await apiClient.post<CreateProductResponse>(
    "/api/v1/admin/products",
    payload
  );
  return data;
}

/** Update the price for a single SKU (ADMIN only). */
export async function updateSkuPrice(
  skuId: number,
  price: number
): Promise<SkuPriceResponse> {
  const { data } = await apiClient.put<SkuPriceResponse>(
    `/api/v1/admin/skus/${skuId}/price`,
    { price }
  );
  return data;
}

/** Update the stock for a single SKU (ADMIN only). */
export async function updateSkuStock(
  skuId: number,
  payload: UpdateStockPayload
): Promise<SkuStockResponse> {
  const { data } = await apiClient.put<SkuStockResponse>(
    `/api/v1/admin/skus/${skuId}/stock`,
    payload
  );
  return data;
}
