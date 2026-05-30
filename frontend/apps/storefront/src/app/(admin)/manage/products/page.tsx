"use client";

import { ProductManagementTable } from "@/features/product-management";

export default function ProductsPage() {
  return (
    <div className="flex flex-col flex-1 gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Products</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Manage your product catalog, variants, and listings.
        </p>
      </div>
      <ProductManagementTable />
    </div>
  );
}
