"use client";

import { OrderManagementTable, OrderKpiRibbon } from "@/features/order-management";

export default function Page() {
  return (
    <div className="flex flex-col flex-1 gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Orders</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Manage customer orders, track fulfillment, and verify stock before shipping.
        </p>
      </div>
      <OrderKpiRibbon />
      <OrderManagementTable />
    </div>
  );
}
