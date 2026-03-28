export type MatchingSize = "RUNS_SMALL" | "RUNS_LARGE" | "ACCURATE";

/** Single approved review shown to storefront visitors. */
export interface StorefrontReview {
  id: number;
  authorName: string;
  rating: number;           // 1–5
  title: string | null;
  body: string;
  pros: string | null;
  cons: string | null;
  matchingSize: MatchingSize | null;
  photoUrls: string[];
  sellerReply: string | null;
  createdAt: string;        // ISO instant
}

/** Rating summary for a variant — returned by GET /listings/{slug}/rating */
export interface RatingSummary {
  averageRating: number;
  reviewCount: number;
  distribution: Record<number, number>;  // { 1: N, 2: N, 3: N, 4: N, 5: N }
  sizeAccurate: number;
  sizeRunsSmall: number;
  sizeRunsLarge: number;
}

/** Paginated reviews response */
export interface ReviewPage {
  content: StorefrontReview[];
  totalElements: number;
  totalPages: number;
  page: number;
}

export type ReviewSortOption = "newest" | "highest" | "lowest";

/** Request body for POST /api/v1/reviews */
export interface SubmitReviewRequest {
  listingVariantId: number;
  skuId: number | null;
  orderId: number;
  rating: number;
  title: string | null;
  body: string;
  pros: string | null;
  cons: string | null;
  matchingSize: MatchingSize | null;
  photoUrls: string[];   // always [] until customer photo upload is implemented
}
