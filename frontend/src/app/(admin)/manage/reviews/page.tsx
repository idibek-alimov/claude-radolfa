"use client";

import { useState } from "react";
import { ReviewModerationQueue } from "@/features/review-moderation";
import { AllReviewsTable } from "@/features/review-moderation/ui/AllReviewsTable";

type Tab = "pending" | "all";

export default function ReviewsPage() {
  const [tab, setTab] = useState<Tab>("pending");

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Reviews</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Moderate customer reviews — approve, reject, or reply.
        </p>
      </div>

      {/* Tab switcher */}
      <div className="flex gap-1 border-b">
        <button
          onClick={() => setTab("pending")}
          className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
            tab === "pending"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          Pending Moderation
        </button>
        <button
          onClick={() => setTab("all")}
          className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
            tab === "all"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          All Reviews
        </button>
      </div>

      {tab === "pending" ? <ReviewModerationQueue /> : <AllReviewsTable />}
    </div>
  );
}
