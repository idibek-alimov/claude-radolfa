"use client";

import { CategoryManagementPanel } from "@/features/category-management";

export default function CategoriesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Categories</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Organize your catalog with a hierarchical category tree.
        </p>
      </div>
      <CategoryManagementPanel />
    </div>
  );
}
