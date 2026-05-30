"use client";

import ProtectedRoute from "@radolfa/shared/components/ProtectedRoute";

export default function CourierLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedRoute requiredRole="COURIER">{children}</ProtectedRoute>;
}
