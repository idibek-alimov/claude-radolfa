"use client";

import ProtectedRoute from "@radolfa/shared/components/ProtectedRoute";
import { PickpointOverviewPage } from "@/features/pickpoint-management/ui/PickpointOverviewPage";

export default function PickpointsPage() {
  return (
    <ProtectedRoute requiredRole="MANAGER">
      <PickpointOverviewPage />
    </ProtectedRoute>
  );
}
