import { apiClient } from "@/shared/api";
import type { Tag } from "../model/types";

/** GET /api/v1/tags — public, no auth required */
export const fetchTags = (): Promise<Tag[]> =>
  apiClient.get("/api/v1/tags").then((r) => r.data);

export interface CreateTagRequest {
  name: string;     // max 64 chars
  colorHex: string; // 6-char hex, no # prefix
}

/** POST /api/v1/admin/tags — ADMIN only, returns 201 + created tag */
export const createTag = (body: CreateTagRequest): Promise<Tag> =>
  apiClient.post("/api/v1/admin/tags", body).then((r) => r.data);

/**
 * PUT /api/v1/admin/variants/{variantId}/tags — MANAGER+
 * Replaces all tags on a variant. Pass empty array to remove all.
 */
export const setVariantTags = (
  variantId: number,
  tagIds: number[]
): Promise<void> =>
  apiClient.put(`/api/v1/admin/variants/${variantId}/tags`, { tagIds });
