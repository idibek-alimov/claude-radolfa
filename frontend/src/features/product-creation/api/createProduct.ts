import { apiClient } from "@/shared/api";
import type { WizardState } from "../model/types";

export interface CreateProductResponse {
  productBaseId: number;
}

export function buildPayload(state: WizardState) {
  return {
    name: state.name,
    categoryId: state.categoryId,
    brandId: state.brandId ?? undefined,
    variants: state.variants.map((v) => ({
      colorId: v.colorId,
      webDescription: state.webDescription || undefined,
      attributes: state.attributes,
      images: v.images,
      skus: v.skus.map(({ sizeLabel, price, stockQuantity, weightKg, widthCm, heightCm, depthCm }) => ({
        sizeLabel,
        price,
        stockQuantity,
        weightKg: weightKg ?? undefined,
        widthCm: widthCm ?? undefined,
        heightCm: heightCm ?? undefined,
        depthCm: depthCm ?? undefined,
      })),
    })),
  };
}

export async function createProduct(
  state: WizardState
): Promise<CreateProductResponse> {
  const { data } = await apiClient.post<CreateProductResponse>(
    "/api/v1/admin/products",
    buildPayload(state)
  );
  return data;
}
