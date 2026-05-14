import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { ProductCreationWizard } from "@/features/product-creation/ui/ProductCreationWizard";

export default function CreateProductPage() {
  return (
    <ProtectedRoute requiredRole="MANAGER">
      <ProductCreationWizard />
    </ProtectedRoute>
  );
}
