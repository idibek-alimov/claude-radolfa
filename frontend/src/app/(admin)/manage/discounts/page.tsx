"use client";

import { DiscountsTab } from "@/features/discount-management";

export default function DiscountsPage() {
  return (
    <div className="flex flex-col flex-1 min-h-0 overflow-hidden gap-6">
      <div className="shrink-0">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Discounts</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Manage discount campaigns, types, and active promotions.
        </p>
      </div>
      <DiscountsTab />
    </div>
  );
}
