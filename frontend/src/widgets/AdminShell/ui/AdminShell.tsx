"use client";

import React from "react";
import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { AdminShellProvider, useAdminShell } from "../model/AdminShellContext";
import { AdminTopBar } from "./AdminTopBar";
import { AdminSidebar } from "./AdminSidebar";
import { cn } from "@/shared/lib";

function AdminShellInner({ children }: { children: React.ReactNode }) {
  const { collapsed } = useAdminShell();

  return (
    <div className="min-h-screen flex flex-col bg-[#F7F7F9]">
      <AdminTopBar />
      <AdminSidebar />
      <main
        className={cn(
          "pt-14 flex-1 flex flex-col overflow-y-auto transition-[padding-left] duration-200 ease-in-out",
          collapsed ? "pl-[56px]" : "pl-[240px]"
        )}
      >
        {children}
      </main>
    </div>
  );
}

export function AdminShell({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute requiredRole="MANAGER">
      <AdminShellProvider>
        <AdminShellInner>{children}</AdminShellInner>
      </AdminShellProvider>
    </ProtectedRoute>
  );
}
