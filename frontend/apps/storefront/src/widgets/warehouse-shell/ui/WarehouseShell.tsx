"use client";

import React from "react";
import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { WarehouseSidebar } from "./WarehouseSidebar";

export function WarehouseShell({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute requiredRole="WAREHOUSE_MANAGER">
      <div className="h-screen overflow-hidden flex bg-[#F7F7F9]">
        <WarehouseSidebar />
        <main className="flex-1 overflow-y-auto p-6">{children}</main>
      </div>
    </ProtectedRoute>
  );
}
