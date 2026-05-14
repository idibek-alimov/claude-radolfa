import { Suspense } from "react";
import { Loader2 } from "lucide-react";
import { PaymentReturnPage } from "@/views/payment/return";

// Public page — payment provider redirects back here (no auth guard needed)
export default function Page() {
  return (
    <Suspense
      fallback={
        <div className="flex justify-center items-center py-32">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      }
    >
      <PaymentReturnPage />
    </Suspense>
  );
}
