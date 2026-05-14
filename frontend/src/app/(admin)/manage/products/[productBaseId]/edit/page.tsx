import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { ProductCardEditPage } from "@/features/product-edit";

interface Props {
  params: Promise<{ productBaseId: string }>;
}

export default async function EditProductPage({ params }: Props) {
  const { productBaseId } = await params;

  return (
    <ProtectedRoute requiredRole="MANAGER">
      <ProductCardEditPage productBaseId={Number(productBaseId)} />
    </ProtectedRoute>
  );
}
