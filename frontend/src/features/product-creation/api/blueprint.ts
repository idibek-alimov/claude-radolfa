import { apiClient } from "@/shared/api";
import type {
  AdminBlueprintEntry,
  BlueprintEntryDto,
  CreateBlueprintEntryRequest,
} from "../model/types";

export async function fetchBlueprint(
  categoryId: number
): Promise<BlueprintEntryDto[]> {
  const { data } = await apiClient.get<BlueprintEntryDto[]>(
    `/api/v1/categories/${categoryId}/blueprint`
  );
  return data;
}

/**
 * GET /api/v1/admin/categories/{categoryId}/blueprint — ADMIN only.
 * Returns full entries including id, type, unitName, allowedValues.
 */
export async function fetchAdminBlueprint(
  categoryId: number
): Promise<AdminBlueprintEntry[]> {
  const { data } = await apiClient.get<AdminBlueprintEntry[]>(
    `/api/v1/admin/categories/${categoryId}/blueprint`
  );
  return data;
}

/**
 * POST /api/v1/admin/categories/{categoryId}/blueprint — ADMIN only.
 * Returns 201; created entry ID is in the Location header (not used by this client).
 */
export async function createBlueprintEntry(
  categoryId: number,
  body: CreateBlueprintEntryRequest
): Promise<void> {
  await apiClient.post(
    `/api/v1/admin/categories/${categoryId}/blueprint`,
    body
  );
}

/**
 * DELETE /api/v1/admin/categories/{categoryId}/blueprint/{blueprintId} — ADMIN only.
 */
export async function deleteBlueprintEntry(
  categoryId: number,
  blueprintId: number
): Promise<void> {
  await apiClient.delete(
    `/api/v1/admin/categories/${categoryId}/blueprint/${blueprintId}`
  );
}
