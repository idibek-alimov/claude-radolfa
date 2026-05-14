import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { DiscountCreationWizard } from "@/features/discount-management/ui/DiscountCreationWizard";

interface Props {
  searchParams: Promise<{ from?: string }>;
}

export default async function CreateDiscountPage({ searchParams }: Props) {
  const { from } = await searchParams;
  const fromId = from ? Number(from) : undefined;
  return (
    <ProtectedRoute requiredRole="MANAGER">
      <DiscountCreationWizard fromId={fromId} />
    </ProtectedRoute>
  );
}
