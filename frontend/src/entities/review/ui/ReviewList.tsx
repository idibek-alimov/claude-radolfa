"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchReviews, fetchRatingSummary } from "../api";
import { ReviewCard } from "./ReviewCard";
import type { ReviewSortOption } from "../model/types";

const PAGE_SIZE = 10;

const sortLabels: Record<ReviewSortOption, string> = {
  newest: "Newest",
  highest: "Highest rated",
  lowest: "Lowest rated",
};

interface ReviewListProps {
  slug: string;
}

export function ReviewList({ slug }: ReviewListProps) {
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState<ReviewSortOption>("newest");

  // Shares the cache entry with RatingSummaryCard — no extra network request.
  const { data: ratingSummary } = useQuery({
    queryKey: ["rating", slug],
    queryFn: () => fetchRatingSummary(slug),
  });

  const totalReviews = ratingSummary?.reviewCount ?? 0;

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["reviews", slug, page, sort],
    queryFn: () => fetchReviews(slug, page, PAGE_SIZE, sort),
    enabled: totalReviews > 0,
  });

  if (totalReviews === 0) return null;

  return (
    <div className="space-y-4">
      {/* Sort pills */}
      <div className="flex gap-2 flex-wrap">
        {(["newest", "highest", "lowest"] as ReviewSortOption[]).map((opt) => (
          <button
            key={opt}
            onClick={() => { setSort(opt); setPage(1); }}
            className={`text-sm px-3 py-1 rounded-full border transition-colors ${
              sort === opt
                ? "bg-foreground text-background border-foreground"
                : "border-border text-muted-foreground hover:border-foreground"
            }`}
          >
            {sortLabels[opt]}
          </button>
        ))}
      </div>

      {/* Review cards */}
      <div className={`transition-opacity ${isFetching && !isLoading ? "opacity-50" : ""}`}>
        {isLoading
          ? Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="space-y-2 py-4 border-b last:border-b-0 animate-pulse">
                <div className="flex items-center gap-2">
                  <div className="h-4 w-24 rounded bg-muted" />
                  <div className="h-4 w-20 rounded bg-muted" />
                </div>
                <div className="h-4 w-3/4 rounded bg-muted" />
                <div className="h-4 w-1/2 rounded bg-muted" />
              </div>
            ))
          : data?.content.map((review) => (
              <ReviewCard key={review.id} review={review} />
            ))}
      </div>

      {/* Pagination */}
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
