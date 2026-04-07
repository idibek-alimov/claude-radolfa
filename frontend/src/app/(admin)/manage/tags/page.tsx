"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { TagListPanel } from "@/features/tag-management";

export default function TagsPage() {
  return (
    <ProtectedRoute requiredRole="ADMIN">
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Tags</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Create and manage product tags for improved discoverability.
          </p>
        </div>
        <TagListPanel />
      </div>
    </ProtectedRoute>
  );
}
