import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { CheckoutPage } from "@/pages/checkout";

export default function Page() {
  return (
    <ProtectedRoute>
      <CheckoutPage />
    </ProtectedRoute>
  );
}
