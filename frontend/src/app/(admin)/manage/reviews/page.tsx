"use client";

import { ReviewModerationQueue } from "@/features/review-moderation";

export default function ReviewsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Reviews</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Moderate customer reviews — approve, reject, or reply.
        </p>
      </div>
      <ReviewModerationQueue />
    </div>
  );
}
