"use client";

import { FeaturedCategoryPanel } from "@/features/featured-category-management";

export default function FeaturedCategoriesPage() {
  return (
    <div className="flex flex-col flex-1 min-h-0 gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Featured Categories</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Curate which categories spotlight on the homepage, with images and order overrides.
          When no entries are active, the homepage falls back to auto-selected categories.
        </p>
      </div>
      <FeaturedCategoryPanel />
    </div>
  );
}
