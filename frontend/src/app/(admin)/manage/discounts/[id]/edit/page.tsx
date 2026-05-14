import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { DiscountCreationWizard } from "@/features/discount-management/ui/DiscountCreationWizard";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function EditDiscountPage({ params }: Props) {
  const { id } = await params;
  return (
    <ProtectedRoute requiredRole="MANAGER">
      <DiscountCreationWizard editId={Number(id)} />
    </ProtectedRoute>
  );
}
