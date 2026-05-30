"use client";

import { useTranslations } from "next-intl";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@radolfa/shared/ui/button";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { useAvailableOrders } from "@/features/courier/api";
import { CourierOrderCard } from "./CourierOrderCard";

const PAGE_SIZE = 20;

interface Props {
  page: number;
  onPageChange: (page: number) => void;
}

export function AvailableTabPanel({ page, onPageChange }: Props) {
  const t = useTranslations("courier");
  const { data, isLoading } = useAvailableOrders(page);

  if (isLoading) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-40 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  if (!data || data.content.length === 0) {
    return (
      <div className="border border-dashed rounded-xl p-10 text-center">
        <p className="text-sm text-muted-foreground">{t("noOrders")}</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {data.content.map((o) => (
        <CourierOrderCard key={o.orderId} order={o} />
      ))}

      {data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground py-2">
          <span>
            {t("showing")} {(page - 1) * PAGE_SIZE + 1}–
            {Math.min(page * PAGE_SIZE, data.totalElements)} {t("of")}{" "}
            {data.totalElements}
          </span>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(page - 1)}
              disabled={page <= 1}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(page + 1)}
              disabled={data.last}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
