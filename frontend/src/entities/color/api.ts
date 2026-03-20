import { apiClient } from "@/shared/api";
import type { Color } from "@/entities/product/model/types";

/** Fetch all colors. Used for product creation form dropdowns. */
export async function fetchColors(): Promise<Color[]> {
  const { data } = await apiClient.get<Color[]>("/api/v1/colors");
  return data;
}
