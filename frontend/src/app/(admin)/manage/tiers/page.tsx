"use client";

import { LoyaltyTierManagementTable } from "@/features/loyalty-tier-management";

export default function TiersPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Loyalty Tiers</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Configure tier colors for the loyalty program display.
        </p>
      </div>
      <LoyaltyTierManagementTable />
    </div>
  );
}
