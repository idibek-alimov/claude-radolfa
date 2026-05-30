"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";

export default function CourierLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedRoute requiredRole="COURIER">{children}</ProtectedRoute>;
}
