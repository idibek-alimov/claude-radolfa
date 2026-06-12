"use client";

import React from "react";
import ProtectedRoute from "@radolfa/shared/components/ProtectedRoute";
import { SellerSidebar } from "./SellerSidebar";

export function SellerShell({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute requiredRole="SELLER">
      <div className="h-screen overflow-hidden flex bg-[#F7F7F9]">
        <SellerSidebar />
        <main className="flex-1 overflow-y-auto p-6">{children}</main>
      </div>
    </ProtectedRoute>
  );
}
