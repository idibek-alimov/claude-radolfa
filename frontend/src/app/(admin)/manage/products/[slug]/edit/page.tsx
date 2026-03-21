import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { ProductEditPage } from "@/features/product-edit";

interface Props {
  params: Promise<{ slug: string }>;
}

export default async function EditProductPage({ params }: Props) {
  const { slug } = await params;

  return (
    <ProtectedRoute requiredRole="MANAGER">
      <ProductEditPage slug={slug} />
    </ProtectedRoute>
  );
}
