"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { QuestionModerationQueue } from "@/features/question-moderation";

export default function QAPage() {
  return (
    <ProtectedRoute requiredRole="MANAGER">
      <div>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-semibold">Q&amp;A Moderation</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Review and respond to customer questions before they appear on product pages.
            </p>
          </div>
        </div>
        <QuestionModerationQueue />
      </div>
    </ProtectedRoute>
  );
}
