"use client";

import { useQueryClient } from "@tanstack/react-query";
import { cn } from "@/shared/lib/utils";
import { Skeleton } from "@/shared/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import { useCourierOrders } from "@/features/courier/api";
import { useDeliverySocket } from "@/shared/lib/useDeliverySocket";
import type { CourierOrder } from "@/entities/user";
import { CourierOrderCard } from "./CourierOrderCard";

function TabBadge({ count, amber }: { count: number; amber?: boolean }) {
  if (count === 0) return null;
  return (
    <span
      className={cn(
        "ml-1.5 rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none",
        amber
          ? "bg-amber-100 text-amber-700"
          : "bg-primary/10 text-primary"
      )}
    >
      {count}
    </span>
  );
}

function EmptyTab() {
  return (
    <div className="border border-dashed rounded-xl p-10 text-center">
      <p className="text-sm text-muted-foreground">No orders here.</p>
    </div>
  );
}

function OrderList({ orders }: { orders: CourierOrder[] }) {
  if (orders.length === 0) return <EmptyTab />;
  return (
    <div className="space-y-4">
      {orders.map((o) => <CourierOrderCard key={o.orderId} order={o} />)}
    </div>
  );
}

export function CourierDashboardPage() {
  const qc = useQueryClient();
  const { data: orders = [], isLoading } = useCourierOrders();
  const { connected } = useDeliverySocket({
    topic: "/user/queue/delivery",
    onMessage: () => qc.invalidateQueries({ queryKey: ["courier-orders"] }),
  });

  const toCollect = orders.filter((o) => o.status === "SHIPPED");
  const inTransit = orders.filter((o) => o.status === "OUT_FOR_DELIVERY");
  const attempted = orders.filter((o) => o.status === "DELIVERY_ATTEMPTED");

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-lg mx-auto px-4 py-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold">My Deliveries</h1>
          <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <span
              className={cn(
                "h-2 w-2 rounded-full",
                connected ? "bg-green-500" : "bg-zinc-400"
              )}
            />
            {connected ? "Live" : "Reconnecting…"}
          </span>
        </div>

        {/* Loading */}
        {isLoading ? (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-52 w-full rounded-xl" />
            ))}
          </div>
        ) : (
          <Tabs defaultValue="collect">
            <TabsList className="w-full">
              <TabsTrigger value="collect" className="flex-1">
                To Collect
                <TabBadge count={toCollect.length} />
              </TabsTrigger>
              <TabsTrigger value="transit" className="flex-1">
                In Transit
                <TabBadge count={inTransit.length} />
              </TabsTrigger>
              <TabsTrigger
                value="attempted"
                className={cn("flex-1", attempted.length > 0 && "text-amber-700")}
              >
                Attempted
                <TabBadge count={attempted.length} amber />
              </TabsTrigger>
            </TabsList>

            <TabsContent value="collect" className="mt-4">
              <OrderList orders={toCollect} />
            </TabsContent>
            <TabsContent value="transit" className="mt-4">
              <OrderList orders={inTransit} />
            </TabsContent>
            <TabsContent value="attempted" className="mt-4">
              <OrderList orders={attempted} />
            </TabsContent>
          </Tabs>
        )}
      </div>
    </div>
  );
}
