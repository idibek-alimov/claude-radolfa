"use client";

import ProtectedRoute from "@radolfa/shared/components/ProtectedRoute";
import { SellerManagementTable } from "@/features/seller-management";

export default function SellersPage() {
  return (
    <ProtectedRoute requiredRole="ADMIN">
      <SellerManagementTable />
    </ProtectedRoute>
  );
}
