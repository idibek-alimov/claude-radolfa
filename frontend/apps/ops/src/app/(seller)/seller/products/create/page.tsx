"use client";

import { ProductCreationWizard } from "@/features/product-creation/ui/ProductCreationWizard";
import { createMyProduct } from "@/entities/seller/api/seller";

export default function SellerCreateProductPage() {
  return (
    <ProductCreationWizard
      createFn={createMyProduct}
      successPath="/seller/products"
    />
  );
}
