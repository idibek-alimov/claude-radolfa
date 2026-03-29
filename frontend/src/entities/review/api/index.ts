import apiClient from "@/shared/api/axios";
import type {
  RatingSummary,
  ReviewAdminView,
  ReviewPage,
  ReviewSortOption,
  SubmitReviewRequest,
} from "../model/types";

/** GET /api/v1/listings/{slug}/rating — public */
export const fetchRatingSummary = (slug: string): Promise<RatingSummary> =>
  apiClient.get(`/api/v1/listings/${slug}/rating`).then((r) => r.data);

/** GET /api/v1/listings/{slug}/reviews — public, paginated */
export const fetchReviews = (
  slug: string,
  page: number,
  size: number,
  sort: ReviewSortOption
): Promise<ReviewPage> =>
  apiClient
    .get(`/api/v1/listings/${slug}/reviews`, { params: { page: page - 1, size, sort } })
    .then((r) => r.data);

/** POST /api/v1/reviews — authenticated users only */
export const submitReview = (body: SubmitReviewRequest): Promise<{ reviewId: number }> =>
  apiClient.post("/api/v1/reviews", body).then((r) => r.data);

/** GET /api/v1/admin/reviews/pending — MANAGER+ */
export const fetchPendingReviews = (): Promise<ReviewAdminView[]> =>
  apiClient.get("/api/v1/admin/reviews/pending").then((r) => r.data);

/** PATCH /api/v1/admin/reviews/{id}/approve — ADMIN only */
export const approveReview = (id: number): Promise<void> =>
  apiClient.patch(`/api/v1/admin/reviews/${id}/approve`);

/** PATCH /api/v1/admin/reviews/{id}/reject — ADMIN only */
export const rejectReview = (id: number): Promise<void> =>
  apiClient.patch(`/api/v1/admin/reviews/${id}/reject`);

/** POST /api/v1/admin/reviews/{id}/reply — MANAGER+ */
export const replyToReview = (id: number, replyText: string): Promise<void> =>
  apiClient.post(`/api/v1/admin/reviews/${id}/reply`, { replyText });

/** POST /api/v1/reviews/upload-photos — authenticated users; max 5 files */
export const uploadReviewPhotos = (files: File[]): Promise<{ urls: string[] }> => {
  const form = new FormData();
  files.forEach((f) => form.append("files", f));
  return apiClient.post("/api/v1/reviews/upload-photos", form).then((r) => r.data);
};
