"use client";

import { useQueryClient } from "@tanstack/react-query";
import { cn } from "@/shared/lib/utils";
import { Skeleton } from "@/shared/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import { useAuth } from "@/features/auth";
import { usePickpointOrders } from "@/features/pickpoint/api";
import { useDeliverySocket } from "@/shared/lib/useDeliverySocket";
import type { PickpointOrder } from "@/entities/user";
import { PickpointOrderCard } from "./PickpointOrderCard";

function TabBadge({ count }: { count: number }) {
  if (count === 0) return null;
  return (
    <span className="ml-1.5 rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none bg-primary/10 text-primary">
      {count}
    </span>
  );
}

function EmptyTab({ message }: { message: string }) {
  return (
    <div className="border border-dashed rounded-xl p-10 text-center">
      <p className="text-sm text-muted-foreground">{message}</p>
    </div>
  );
}

function OrderList({ orders, emptyMessage }: { orders: PickpointOrder[]; emptyMessage: string }) {
  if (orders.length === 0) return <EmptyTab message={emptyMessage} />;
  return (
    <div className="space-y-4">
      {orders.map((o) => <PickpointOrderCard key={o.orderId} order={o} />)}
    </div>
  );
}

export function PickpointDashboardPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const { data: orders = [], isLoading } = usePickpointOrders();

  const topic = user?.pickpointId
    ? `/topic/pickpoint/${user.pickpointId}`
    : "__disabled__";

  const { connected } = useDeliverySocket({
    topic,
    onMessage: () => qc.invalidateQueries({ queryKey: ["pickpoint-orders"] }),
  });

  const awaiting = orders.filter((o) => o.status === "READY_FOR_PICKUP");
  const history  = orders.filter((o) => o.status === "DELIVERED");

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-lg mx-auto px-4 py-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold">
            {user?.pickpointName ?? "My Pickpoint"}
          </h1>
          <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <span className={cn("h-2 w-2 rounded-full", connected ? "bg-green-500" : "bg-zinc-400")} />
            {connected ? "Live" : "Reconnecting…"}
          </span>
        </div>

        {/* Loading */}
        {isLoading ? (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-40 w-full rounded-xl" />
            ))}
          </div>
        ) : (
          <Tabs defaultValue="awaiting">
            <TabsList className="w-full">
              <TabsTrigger value="awaiting" className="flex-1">
                Awaiting Pickup
                <TabBadge count={awaiting.length} />
              </TabsTrigger>
              <TabsTrigger value="history" className="flex-1">
                History
                <TabBadge count={history.length} />
              </TabsTrigger>
            </TabsList>

            <TabsContent value="awaiting" className="mt-4">
              <OrderList orders={awaiting} emptyMessage="No orders awaiting pickup." />
            </TabsContent>
            <TabsContent value="history" className="mt-4">
              <OrderList orders={history} emptyMessage="No pickups completed today." />
            </TabsContent>
          </Tabs>
        )}
      </div>
    </div>
  );
}
