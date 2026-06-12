"use client";

import ProtectedRoute from "@radolfa/shared/components/ProtectedRoute";

export default function PickpointLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedRoute requiredRole="PICKPOINT_STAFF">{children}</ProtectedRoute>;
}
