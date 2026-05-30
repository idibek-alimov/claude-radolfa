"use client";

import { ColorManagementTable } from "@/features/color-management";

export default function ColorsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Colors</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Edit display names and hex values for product color variants.
        </p>
      </div>
      <ColorManagementTable />
    </div>
  );
}
