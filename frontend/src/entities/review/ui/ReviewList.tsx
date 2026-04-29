"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { fetchReviews, fetchRatingSummary } from "../api";
import { ReviewCard } from "./ReviewCard";
import { ReviewPhotoStrip } from "./ReviewPhotoStrip";
import type { ReviewSortOption } from "../model/types";
import type { ReviewFilters } from "../model/filters";
import { defaultReviewFilters } from "../model/filters";
import { Input } from "@/shared/ui/input";
import { useDebounce } from "@/shared/lib";

const PREVIEW_SIZE = 6;
const FULL_SIZE = 10;
const CARDS_PER_SLIDE = 3;

interface ReviewListProps {
  slug: string;
  mode?: "preview" | "full";
  filters?: ReviewFilters;
  onFiltersChange?: (next: ReviewFilters) => void;
  showSearch?: boolean;
}

export function ReviewList({
  slug,
  mode = "preview",
  filters: externalFilters,
  onFiltersChange,
  showSearch = false,
}: ReviewListProps) {
  const t = useTranslations("reviews");
  const tCommon = useTranslations("common");
  const router = useRouter();

  // Uncontrolled internal state (used when no external filters are wired)
  const [internalSort, setInternalSort] = useState<ReviewSortOption>("newest");
  const [internalHasPhotos, setInternalHasPhotos] = useState(false);
  const [carouselPage, setCarouselPage] = useState(0);
  const [page, setPage] = useState(1);

  const controlled = externalFilters != null && onFiltersChange != null;
  const activeFilters: ReviewFilters = controlled ? externalFilters : defaultReviewFilters;

  const sort = controlled ? activeFilters.sort : internalSort;
  const hasPhotos = controlled ? activeFilters.hasPhotos : internalHasPhotos;
  const rating = controlled ? activeFilters.rating : null;
  const searchInput = controlled ? activeFilters.search : "";
  const debouncedSearch = useDebounce(searchInput, 500);

  const handleFilterChange = (partial: Partial<ReviewFilters>) => {
    if (controlled) {
      onFiltersChange({ ...activeFilters, ...partial });
    } else {
      if (partial.sort !== undefined) setInternalSort(partial.sort as ReviewSortOption);
      if (partial.hasPhotos !== undefined) setInternalHasPhotos(partial.hasPhotos as boolean);
    }
    setPage(1);
  };

  const clearFilters = () => {
    if (controlled) {
      onFiltersChange({ ...activeFilters, rating: null, search: "", hasPhotos: false });
    } else {
      setInternalHasPhotos(false);
    }
    setPage(1);
  };

  // Shares cache with RatingSummaryCard — no extra network request.
  const { data: ratingSummary } = useQuery({
    queryKey: ["rating", slug],
    queryFn: () => fetchRatingSummary(slug),
  });

  const totalReviews = ratingSummary?.reviewCount ?? 0;

  const fetchPage = mode === "preview" ? 1 : page;
  const fetchSort = mode === "preview" ? "newest" : sort;
  const fetchSize = mode === "preview" ? PREVIEW_SIZE : FULL_SIZE;
  const fetchHasPhotos = mode === "preview" ? undefined : (hasPhotos || undefined);
  const fetchRating = mode === "preview" ? undefined : (rating ?? undefined);
  const fetchSearch = mode === "preview" ? undefined : (debouncedSearch || undefined);

  const isAnyFilterActive =
    mode === "full" && (rating != null || !!(debouncedSearch?.trim()) || hasPhotos);

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["reviews", slug, fetchPage, fetchSort, fetchSize, fetchHasPhotos, fetchRating, fetchSearch],
    queryFn: () => fetchReviews(slug, fetchPage, fetchSize, fetchSort, fetchHasPhotos, fetchRating, fetchSearch),
    enabled: totalReviews > 0,
  });

  if (totalReviews === 0) return null;

  const allPhotos = data?.content.flatMap((r) => r.photoUrls) ?? [];
  const reviewsPageUrl = `/products/${slug}/reviews`;

  const sortOptions: { value: ReviewSortOption; label: string }[] = [
    { value: "newest", label: t("sort.newest") },
    { value: "highest", label: t("sort.highestRated") },
    { value: "lowest", label: t("sort.lowestRated") },
  ];

  // ── Preview mode ──────────────────────────────────────────────────────────
  if (mode === "preview") {
    const content = data?.content ?? [];
    const totalSlides = Math.ceil(content.length / CARDS_PER_SLIDE);
    const visibleCards = content.slice(
      carouselPage * CARDS_PER_SLIDE,
      (carouselPage + 1) * CARDS_PER_SLIDE,
    );

    return (
      <div className="space-y-4">
        <ReviewPhotoStrip
          photoUrls={allPhotos}
          totalCount={totalReviews}
          onSeeAll={() => router.push(reviewsPageUrl)}
        />

        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="rounded-xl border p-4 h-36 animate-pulse bg-muted" />
            ))}
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {visibleCards.map((review) => (
                <ReviewCard key={review.id} review={review} />
              ))}
            </div>

            {totalSlides > 1 && (
              <div className="flex justify-center items-center gap-3">
                <button
                  disabled={carouselPage === 0}
                  onClick={() => setCarouselPage((p) => p - 1)}
                  className="p-1 rounded-full border hover:bg-muted transition-colors disabled:opacity-30"
                  aria-label="Previous slide"
                >
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <span className="text-xs text-muted-foreground">
                  {carouselPage + 1} / {totalSlides}
                </span>
                <button
                  disabled={carouselPage === totalSlides - 1}
                  onClick={() => setCarouselPage((p) => p + 1)}
                  className="p-1 rounded-full border hover:bg-muted transition-colors disabled:opacity-30"
                  aria-label="Next slide"
                >
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            )}
          </>
        )}

        <div className="flex justify-center">
          <Link
            href={reviewsPageUrl}
            className="text-sm px-5 py-2 rounded-full border border-primary text-primary hover:bg-primary hover:text-primary-foreground transition-colors"
          >
            {t("seeAll")}
          </Link>
        </div>
      </div>
    );
  }

  // ── Full mode ─────────────────────────────────────────────────────────────
  return (
    <div className="space-y-4">
      <ReviewPhotoStrip photoUrls={allPhotos} />

      {/* Search input */}
      {showSearch && (
        <Input
          type="search"
          placeholder={t("filter.searchPlaceholder")}
          value={searchInput}
          onChange={(e) => {
            if (controlled) {
              onFiltersChange({ ...activeFilters, search: e.target.value });
              setPage(1);
            }
          }}
          className="max-w-sm"
        />
      )}

      {/* Sort + filter pills */}
      <div className="flex items-center gap-2 overflow-x-auto scrollbar-hide pb-1">
        {sortOptions.map(({ value, label }) => (
          <button
            key={value}
            onClick={() => handleFilterChange({ sort: value })}
            className={`text-sm px-3 py-1 rounded-full border transition-colors ${
              sort === value
                ? "bg-foreground text-background border-foreground"
                : "border-border text-muted-foreground hover:border-foreground"
            }`}
          >
            {label}
          </button>
        ))}
        <div className="w-px h-5 bg-border mx-1 hidden sm:block" />
        <button
          onClick={() => handleFilterChange({ hasPhotos: !hasPhotos })}
          className={`text-sm px-3 py-1 rounded-full border transition-colors ${
            hasPhotos
              ? "bg-primary text-primary-foreground border-primary"
              : "border-border text-muted-foreground hover:border-foreground"
          }`}
        >
          {t("sort.withPhoto")}
        </button>

        {isAnyFilterActive && (
          <>
            <div className="w-px h-5 bg-border mx-1 hidden sm:block" />
            <button
              onClick={clearFilters}
              className="flex items-center gap-1 text-sm px-3 py-1 rounded-full border border-border text-muted-foreground hover:border-destructive hover:text-destructive transition-colors"
            >
              <X className="h-3 w-3" />
              {t("filter.clearFilters")}
            </button>
          </>
        )}
      </div>

      <div className={`transition-opacity ${isFetching && !isLoading ? "opacity-50" : ""}`}>
        {isLoading
          ? Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="rounded-xl border p-4 h-36 animate-pulse bg-muted" />
            ))
          : data?.content.length === 0
            ? (
              <div className="py-8 text-center space-y-3">
                <p className="text-sm text-muted-foreground">
                  {isAnyFilterActive ? t("filter.noResults") : t("empty")}
                </p>
                {isAnyFilterActive && (
                  <button
                    onClick={clearFilters}
                    className="text-sm text-primary hover:underline"
                  >
                    {t("filter.clearFilters")}
                  </button>
                )}
              </div>
            )
            : data?.content.map((review) => (
                <ReviewCard key={review.id} review={review} showSellerReply variant="list" />
              ))}
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex justify-center items-center gap-2">
          <button
            disabled={page === 1}
            onClick={() => setPage((p) => p - 1)}
            className="px-3 py-1 text-sm border rounded disabled:opacity-30 hover:bg-muted transition-colors"
          >
            {tCommon("pagination.previous")}
          </button>
          <span className="px-3 py-1 text-sm text-muted-foreground">
            {page} / {data.totalPages}
          </span>
          <button
            disabled={page === data.totalPages}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1 text-sm border rounded disabled:opacity-30 hover:bg-muted transition-colors"
          >
            {tCommon("pagination.next")}
          </button>
        </div>
      )}
    </div>
  );
}
