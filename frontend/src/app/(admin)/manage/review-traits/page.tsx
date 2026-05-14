"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { ReviewTraitsPanel } from "@/features/review-trait-management";

export default function ReviewTraitsPage() {
  return (
    <ProtectedRoute requiredRole="ADMIN">
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Review Traits</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage the global bank of category-specific review questions.
          </p>
        </div>
        <ReviewTraitsPanel />
      </div>
    </ProtectedRoute>
  );
}
