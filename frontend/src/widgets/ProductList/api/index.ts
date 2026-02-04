import { apiClient } from "@/shared/api";
import type { Product } from "@/entities/product";

export interface PaginatedProducts {
  products: Product[];
  total: number;
  page: number;
  hasMore: boolean;
}

/** Paginated product catalog. */
export async function fetchProducts(
  page: number = 1,
  limit: number = 12
): Promise<PaginatedProducts> {
  const { data } = await apiClient.get<PaginatedProducts>(
    "/api/v1/products",
    { params: { page, limit } }
  );
  return data;
}

/** Homepage shortcut: only products flagged as top sellers. */
export async function fetchTopSellingProducts(): Promise<Product[]> {
  const { data } = await apiClient.get<Product[]>(
    "/api/v1/products/top-selling"
  );
  return data;
}
