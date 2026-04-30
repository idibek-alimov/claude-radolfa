import { apiClient } from "@/shared/api";
import type {
  ReviewTrait,
  CreateReviewTraitRequest,
  UpdateReviewTraitRequest,
} from "../model/types";

export async function fetchReviewTraits(): Promise<ReviewTrait[]> {
  const { data } = await apiClient.get<ReviewTrait[]>("/api/v1/admin/review-traits");
  return data;
}

export async function createReviewTrait(
  body: CreateReviewTraitRequest
): Promise<{ traitId: number }> {
  const { data } = await apiClient.post<{ traitId: number }>(
    "/api/v1/admin/review-traits",
    body
  );
  return data;
}

export async function updateReviewTrait(
  id: number,
  body: UpdateReviewTraitRequest
): Promise<void> {
  await apiClient.patch(`/api/v1/admin/review-traits/${id}`, body);
}

export async function deleteReviewTrait(id: number): Promise<void> {
  await apiClient.delete(`/api/v1/admin/review-traits/${id}`);
}
