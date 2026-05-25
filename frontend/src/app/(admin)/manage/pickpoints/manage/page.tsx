"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { PickpointManagementPanel } from "@/features/pickpoint-management";

export default function PickpointsPage() {
  return (
    <ProtectedRoute requiredRole="ADMIN">
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Pickpoints</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage physical pickup locations available at checkout.
          </p>
        </div>
        <PickpointManagementPanel />
      </div>
    </ProtectedRoute>
  );
}
