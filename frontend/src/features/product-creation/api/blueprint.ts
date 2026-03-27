import { apiClient } from "@/shared/api";
import type { BlueprintEntryDto } from "../model/types";

export async function fetchBlueprint(
  categoryId: number
): Promise<BlueprintEntryDto[]> {
  const { data } = await apiClient.get<BlueprintEntryDto[]>(
    `/api/v1/categories/${categoryId}/blueprint`
  );
  return data;
}
