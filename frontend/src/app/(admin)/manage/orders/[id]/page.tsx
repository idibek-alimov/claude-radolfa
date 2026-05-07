"use client";

import { useParams, useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/shared/ui/button";

export default function Page() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();

  return (
    <div className="flex flex-col flex-1 gap-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => router.push("/manage/orders")}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          Back to Orders
        </Button>
      </div>
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Order #{id}</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Detail view coming in Phase 4.
        </p>
      </div>
    </div>
  );
}
