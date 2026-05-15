import { Suspense } from "react";
import { Loader2 } from "lucide-react";
import { BankPaymentPage } from "@/views/payment/bank";

export const metadata = {
  title: "Bank of Radolfa | Secure Payment",
  robots: { index: false, follow: false },
};

export default function Page() {
  return (
    <Suspense
      fallback={
        <div className="flex justify-center items-center min-h-screen">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      }
    >
      <BankPaymentPage />
    </Suspense>
  );
}
