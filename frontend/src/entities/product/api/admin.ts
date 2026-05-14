import { apiClient } from "@/shared/api";
import type { ProductCard } from "@/entities/product/model/types";

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

/** Update the name of a product base (MANAGER+). */
export async function updateProductName(
  productBaseId: number,
  name: string
): Promise<void> {
  await apiClient.patch(`/api/v1/admin/products/${productBaseId}/name`, { name });
}

/** Update the size label of a single SKU (MANAGER+). */
export async function updateSkuSizeLabel(
  skuId: number,
  sizeLabel: string
): Promise<void> {
  await apiClient.patch(`/api/v1/admin/skus/${skuId}/size-label`, { sizeLabel });
}

/** Update the category of a product base (MANAGER+). */
export async function updateProductCategory(
  productBaseId: number,
  categoryId: number
): Promise<void> {
  await apiClient.patch(`/api/v1/admin/products/${productBaseId}/category`, { categoryId });
}

/** Fetch the full admin product card — base + all variants + images + SKUs (MANAGER+). */
export async function fetchProductCard(productBaseId: number): Promise<ProductCard> {
  const { data } = await apiClient.get<ProductCard>(
    `/api/v1/admin/products/${productBaseId}`
  );
  return data;
}

/** Persist a new image sort order for a variant (MANAGER+). */
export async function reorderVariantImages(
  variantId: number,
  imageIds: number[]
): Promise<void> {
  await apiClient.patch(`/api/v1/admin/listings/${variantId}/images/order`, { imageIds });
}

/** Add a new SKU to an existing variant (ADMIN only — price/stock are ADMIN fields). */
export async function addSkuToVariant(
  productBaseId: number,
  variantId: number,
  payload: { sizeLabel: string; price: number; stockQuantity: number }
): Promise<{ skuId: number }> {
  const { data } = await apiClient.post<{ skuId: number }>(
    `/api/v1/admin/products/${productBaseId}/variants/${variantId}/skus`,
    payload
  );
  return data;
}

/** Add a new empty color variant to an existing product base (MANAGER+). */
export async function addVariantToProduct(
  productBaseId: number,
  colorId: number
): Promise<{ variantId: number; slug: string }> {
  const { data } = await apiClient.post<{ variantId: number; slug: string }>(
    `/api/v1/admin/products/${productBaseId}/variants`,
    { colorId }
  );
  return data;
}
