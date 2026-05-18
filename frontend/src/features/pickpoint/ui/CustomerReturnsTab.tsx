"use client";

import Link from "next/link";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import { usePickpointCustomerReturns } from "@/features/pickpoint/api";
import { CustomerReturnCard } from "./CustomerReturnCard";

function EmptyTab({ message }: { message: string }) {
  return (
    <div className="border border-dashed rounded-xl p-10 text-center">
      <p className="text-sm text-muted-foreground">{message}</p>
    </div>
  );
}

export function CustomerReturnsTab() {
  const { data, isLoading } = usePickpointCustomerReturns("RECEIVED");
  const returns = data?.content ?? [];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm font-semibold text-muted-foreground">
          {data ? `${data.totalElements} return${data.totalElements !== 1 ? "s" : ""}` : ""}
        </p>
        <Button asChild>
          <Link href="/pickpoint/customer-returns/new">Log New Return</Link>
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 2 }).map((_, i) => (
            <Skeleton key={i} className="h-36 w-full rounded-xl" />
          ))}
        </div>
      ) : returns.length === 0 ? (
        <EmptyTab message="No customer returns awaiting carrier." />
      ) : (
        <div className="space-y-4">
          {returns.map((r) => (
            <CustomerReturnCard key={r.id} customerReturn={r} />
          ))}
        </div>
      )}
    </div>
  );
}
