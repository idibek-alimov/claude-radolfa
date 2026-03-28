import apiClient from "@/shared/api/axios";
import type {
  RatingSummary,
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
    .get(`/api/v1/listings/${slug}/reviews`, { params: { page, size, sort } })
    .then((r) => r.data);

/** POST /api/v1/reviews — authenticated users only */
export const submitReview = (body: SubmitReviewRequest): Promise<{ reviewId: number }> =>
  apiClient.post("/api/v1/reviews", body).then((r) => r.data);
