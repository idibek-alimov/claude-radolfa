"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { CustomerReturnsQueuePage } from "@/features/customer-return-management/ui/CustomerReturnsQueuePage";

export default function CustomerReturnsPage() {
  return (
    <ProtectedRoute requiredRole="MANAGER">
      <CustomerReturnsQueuePage />
    </ProtectedRoute>
  );
}
