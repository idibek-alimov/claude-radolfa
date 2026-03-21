import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { CreateProductPage } from "@/features/product-creation-page";

export default function NewProductPage() {
  return (
    <ProtectedRoute requiredRole="MANAGER">
      <CreateProductPage />
    </ProtectedRoute>
  );
}
