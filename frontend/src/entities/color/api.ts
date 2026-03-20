import { apiClient } from "@/shared/api";
import type { Color } from "@/entities/product/model/types";

/** Fetch all colors. Used for product creation form dropdowns. */
export async function fetchColors(): Promise<Color[]> {
  const { data } = await apiClient.get<Color[]>("/api/v1/colors");
  return data;
}

/** Update a color's display name and hex code (MANAGER+). */
export async function updateColor(
  colorId: number,
  displayName: string,
  hexCode: string
): Promise<Color> {
  const { data } = await apiClient.patch<Color>(`/api/v1/colors/${colorId}`, {
    displayName,
    hexCode,
  });
  return data;
}
