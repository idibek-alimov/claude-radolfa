import ProtectedRoute from "@radolfa/shared/components/ProtectedRoute";
import { CheckoutPage } from "@/views/checkout";

export default function Page() {
  return (
    <ProtectedRoute>
      <CheckoutPage />
    </ProtectedRoute>
  );
}
