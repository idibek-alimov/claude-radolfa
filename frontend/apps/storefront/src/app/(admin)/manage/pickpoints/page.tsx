"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { PickpointOverviewPage } from "@/features/pickpoint-management/ui/PickpointOverviewPage";

export default function PickpointsPage() {
  return (
    <ProtectedRoute requiredRole="MANAGER">
      <PickpointOverviewPage />
    </ProtectedRoute>
  );
}
