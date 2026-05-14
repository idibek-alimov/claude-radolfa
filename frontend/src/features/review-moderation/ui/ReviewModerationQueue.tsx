"use client";

import { useQuery } from "@tanstack/react-query";
import { Loader2, Inbox } from "lucide-react";
import { fetchPendingReviews } from "@/entities/review";
import { ReviewModerationCard } from "./ReviewModerationCard";

export function ReviewModerationQueue() {
  const { data: reviews = [], isLoading } = useQuery({
    queryKey: ["pending-reviews"],
    queryFn: fetchPendingReviews,
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Pending Reviews</h2>
        {!isLoading && (
          <span className="text-sm text-muted-foreground">
            {reviews.length} awaiting moderation
          </span>
        )}
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
        </div>
      ) : reviews.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-muted-foreground gap-2">
          <Inbox className="h-8 w-8" />
          <p className="text-sm">No reviews pending moderation.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {reviews.map((review) => (
            <ReviewModerationCard key={review.id} review={review} />
          ))}
        </div>
      )}
    </div>
  );
}
