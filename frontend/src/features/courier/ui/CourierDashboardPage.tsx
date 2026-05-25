"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { cn } from "@/shared/lib/utils";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import type { OrderStatus } from "@/entities/order/model/types";
import { useCourierOrders } from "@/features/courier/api";
import { useDeliverySocket } from "@/shared/lib/useDeliverySocket";
import { CourierTabPanel } from "./CourierTabPanel";

function TabBadge({ count, amber }: { count: number; amber?: boolean }) {
  if (count === 0) return null;
  return (
    <span
      className={cn(
        "ml-1.5 rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none",
        amber ? "bg-amber-100 text-amber-700" : "bg-primary/10 text-primary",
      )}
    >
      {count}
    </span>
  );
}

const COLLECT_STATUSES: OrderStatus[] = ["SHIPPED"];
const TRANSIT_STATUSES: OrderStatus[] = ["OUT_FOR_DELIVERY"];
const ATTEMPTED_STATUSES: OrderStatus[] = ["DELIVERY_ATTEMPTED"];

export function CourierDashboardPage() {
  const qc = useQueryClient();
  const t = useTranslations("courier");
  const [pages, setPages] = useState({ collect: 1, transit: 1, attempted: 1 });

  // Page-1 queries for badge counts — deduped with CourierTabPanel's queries at page=1
  const collectCount = useCourierOrders(COLLECT_STATUSES, 1);
  const transitCount = useCourierOrders(TRANSIT_STATUSES, 1);
  const attemptedCount = useCourierOrders(ATTEMPTED_STATUSES, 1);

  const { connected } = useDeliverySocket({
    topic: "/user/queue/delivery",
    onMessage: () => qc.invalidateQueries({ queryKey: ["courier-orders"] }),
  });

  const attemptedTotal = attemptedCount.data?.totalElements ?? 0;

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-lg mx-auto px-4 py-6 space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold">{t("title")}</h1>
          <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <span
              className={cn(
                "h-2 w-2 rounded-full",
                connected ? "bg-green-500" : "bg-zinc-400",
              )}
            />
            {connected ? "Live" : "Reconnecting…"}
          </span>
        </div>

        <Tabs defaultValue="collect">
          <TabsList className="w-full">
            <TabsTrigger value="collect" className="flex-1">
              {t("tab.toCollect")}
              <TabBadge count={collectCount.data?.totalElements ?? 0} />
            </TabsTrigger>
            <TabsTrigger value="transit" className="flex-1">
              {t("tab.inTransit")}
              <TabBadge count={transitCount.data?.totalElements ?? 0} />
            </TabsTrigger>
            <TabsTrigger
              value="attempted"
              className={cn("flex-1", attemptedTotal > 0 && "text-amber-700")}
            >
              {t("tab.attempted")}
              <TabBadge count={attemptedTotal} amber />
            </TabsTrigger>
          </TabsList>

          <TabsContent value="collect" className="mt-4">
            <CourierTabPanel
              statuses={COLLECT_STATUSES}
              page={pages.collect}
              onPageChange={(p) => setPages((prev) => ({ ...prev, collect: p }))}
              emptyMessage={t("noOrders")}
            />
          </TabsContent>

          <TabsContent value="transit" className="mt-4">
            <CourierTabPanel
              statuses={TRANSIT_STATUSES}
              page={pages.transit}
              onPageChange={(p) => setPages((prev) => ({ ...prev, transit: p }))}
              emptyMessage={t("noOrders")}
            />
          </TabsContent>

          <TabsContent value="attempted" className="mt-4">
            <CourierTabPanel
              statuses={ATTEMPTED_STATUSES}
              page={pages.attempted}
              onPageChange={(p) =>
                setPages((prev) => ({ ...prev, attempted: p }))
              }
              emptyMessage={t("noOrders")}
            />
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
