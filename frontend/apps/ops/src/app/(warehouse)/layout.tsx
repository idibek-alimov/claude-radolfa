import type { Metadata } from "next";
import React from "react";
import { WarehouseShell } from "@/widgets/warehouse-shell";

export const metadata: Metadata = {
  title: "Warehouse — Radolfa",
  robots: { index: false, follow: false },
};

export default function WarehouseLayout({ children }: { children: React.ReactNode }) {
  return <WarehouseShell>{children}</WarehouseShell>;
}
