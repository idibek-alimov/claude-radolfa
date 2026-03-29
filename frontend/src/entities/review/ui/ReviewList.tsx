"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { fetchReviews, fetchRatingSummary } from "../api";
import { ReviewCard } from "./ReviewCard";
import { ReviewPhotoStrip } from "./ReviewPhotoStrip";
import type { ReviewSortOption } from "../model/types";

const PREVIEW_SIZE = 6;
const FULL_SIZE = 10;
const CARDS_PER_SLIDE = 3;

interface ReviewListProps {
  slug: string;
  mode?: "preview" | "full";
}

export function ReviewList({ slug, mode = "preview" }: ReviewListProps) {
  const t = useTranslations("reviews");
  const router = useRouter();

  const [page, setPage] = useState(1);
  const [sort, setSort] = useState<ReviewSortOption>("newest");
  const [carouselPage, setCarouselPage] = useState(0);

  // Shares cache with RatingSummaryCard — no extra network request.
  const { data: ratingSummary } = useQuery({
    queryKey: ["rating", slug],
    queryFn: () => fetchRatingSummary(slug),
  });

  const totalReviews = ratingSummary?.reviewCount ?? 0;

  const fetchPage = mode === "preview" ? 1 : page;
  const fetchSort = mode === "preview" ? "newest" : sort;
  const fetchSize = mode === "preview" ? PREVIEW_SIZE : FULL_SIZE;

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["reviews", slug, fetchPage, fetchSort, fetchSize],
    queryFn: () => fetchReviews(slug, fetchPage, fetchSize, fetchSort),
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

      <div className="flex gap-2 flex-wrap">
        {sortOptions.map(({ value, label }) => (
          <button
            key={value}
            onClick={() => { setSort(value); setPage(1); }}
            className={`text-sm px-3 py-1 rounded-full border transition-colors ${
              sort === value
                ? "bg-foreground text-background border-foreground"
                : "border-border text-muted-foreground hover:border-foreground"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className={`space-y-4 transition-opacity ${isFetching && !isLoading ? "opacity-50" : ""}`}>
        {isLoading
          ? Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="rounded-xl border p-4 h-36 animate-pulse bg-muted" />
            ))
          : data?.content.length === 0
            ? <p className="py-4 text-sm text-muted-foreground">{t("empty")}</p>
            : data?.content.map((review) => (
                <ReviewCard key={review.id} review={review} showSellerReply />
              ))}
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex justify-center items-center gap-2">
          <button
            disabled={page === 1}
            onClick={() => setPage((p) => p - 1)}
            className="px-3 py-1 text-sm border rounded disabled:opacity-30 hover:bg-muted transition-colors"
          >
            Previous
          </button>
          <span className="px-3 py-1 text-sm text-muted-foreground">
            {page} / {data.totalPages}
          </span>
          <button
            disabled={page === data.totalPages}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1 text-sm border rounded disabled:opacity-30 hover:bg-muted transition-colors"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
