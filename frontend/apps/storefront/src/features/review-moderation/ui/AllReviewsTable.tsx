"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Loader2, Star, ChevronLeft, ChevronRight } from "lucide-react";
import { fetchAllAdminReviews } from "@/entities/review";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { ReplyDialog } from "./ReplyDialog";
import Link from "next/link";

const STATUS_BADGE: Record<string, { label: string; className: string }> = {
  APPROVED: { label: "Approved", className: "bg-green-100 text-green-700" },
  PENDING:  { label: "Pending",  className: "bg-orange-100 text-orange-700" },
  REJECTED: { label: "Rejected", className: "bg-rose-100 text-rose-700" },
};

const STATUS_FILTERS = ["", "PENDING", "APPROVED", "REJECTED"] as const;
const STATUS_FILTER_LABELS: Record<string, string> = {
  "": "All",
  PENDING:  "Pending",
  APPROVED: "Approved",
  REJECTED: "Rejected",
};

export function AllReviewsTable() {
  const [page, setPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["admin-reviews-all", page, statusFilter],
    queryFn: () => fetchAllAdminReviews(page, 20, statusFilter || undefined),
  });

  const reviews = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="space-y-4">
      {/* Status filter pills */}
      <div className="flex gap-2 flex-wrap">
        {STATUS_FILTERS.map((s) => (
          <button
            key={s}
            onClick={() => { setStatusFilter(s); setPage(1); }}
            className={`px-3 py-1 text-sm rounded-full border transition-colors ${
              statusFilter === s
                ? "bg-primary text-primary-foreground border-primary"
                : "border-border text-muted-foreground hover:border-foreground"
            }`}
          >
            {STATUS_FILTER_LABELS[s]}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
        </div>
      ) : reviews.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-muted-foreground gap-2">
          <p className="text-sm">No reviews found.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {reviews.map((review) => {
            const badge = STATUS_BADGE[review.status] ?? STATUS_BADGE.PENDING;
            return (
              <div key={review.id} className="rounded-xl border bg-card p-4 space-y-2">
                <div className="flex items-start justify-between gap-3">
                  <div className="space-y-0.5 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-sm font-semibold">{review.authorName}</span>
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${badge.className}`}>
                        {badge.label}
                      </span>
                      <div className="flex gap-0.5">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <Star
                            key={i}
                            className={`h-3 w-3 ${i < review.rating ? "text-amber-400 fill-amber-400" : "text-muted-foreground"}`}
                          />
                        ))}
                      </div>
                    </div>
                    <Link
                      href={`/products/${review.variantSlug}`}
                      className="text-xs text-primary hover:underline truncate block"
                      target="_blank"
                    >
                      {review.variantSlug}
                    </Link>
                  </div>
                  <span className="text-xs text-muted-foreground shrink-0">
                    {new Date(review.createdAt).toLocaleDateString()}
                  </span>
                </div>

                {review.title && (
                  <p className="text-sm font-medium">{review.title}</p>
                )}
                <p className="text-sm text-muted-foreground line-clamp-3">{review.body}</p>

                {review.sellerReply && (
                  <p className="text-xs text-muted-foreground border-l-2 border-primary pl-2 italic">
                    {review.sellerReply}
                  </p>
                )}

                {review.status === "APPROVED" && (
                  <div className="flex justify-end pt-1">
                    <ReplyDialog reviewId={review.id} existingReply={review.sellerReply} />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 1}
            onClick={() => setPage((p) => p - 1)}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm text-muted-foreground">
            {page} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page === totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}
    </div>
  );
}
