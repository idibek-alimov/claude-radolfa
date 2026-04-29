import type { ReviewSortOption } from "./types";

export interface ReviewFilters {
  rating: number | null;
  search: string;
  hasPhotos: boolean;
  sort: ReviewSortOption;
}

export const defaultReviewFilters: ReviewFilters = {
  rating: null,
  search: "",
  hasPhotos: false,
  sort: "newest",
};
