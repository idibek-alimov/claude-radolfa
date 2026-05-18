"use client";

import { useQueryClient } from "@tanstack/react-query";
import { cn } from "@/shared/lib/utils";
import { Skeleton } from "@/shared/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import { useAuth } from "@/features/auth";
import {
  usePickpointOrders,
  usePickpointCustomerReturns,
} from "@/features/pickpoint/api";
import { useDeliverySocket } from "@/shared/lib/useDeliverySocket";
import type { PickpointOrder } from "@/entities/user";
import { IncomingPackageCard } from "./IncomingPackageCard";
import { PickpointOrderCard } from "./PickpointOrderCard";
import { ReturnInProgressCard } from "./ReturnInProgressCard";
import { CustomerReturnsTab } from "./CustomerReturnsTab";

function TabBadge({
  count,
  tone = "default",
}: {
  count: number;
  tone?: "default" | "amber";
}) {
  if (count === 0) return null;
  return (
    <span
      className={cn(
        "ml-1.5 rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none",
        tone === "amber"
          ? "bg-amber-100 text-amber-700"
          : "bg-primary/10 text-primary",
      )}
    >
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

function OrderList<T extends PickpointOrder>({
  orders,
  emptyMessage,
  renderCard,
}: {
  orders: T[];
  emptyMessage: string;
  renderCard: (order: T) => React.ReactNode;
}) {
  if (orders.length === 0) return <EmptyTab message={emptyMessage} />;
  return <div className="space-y-4">{orders.map(renderCard)}</div>;
}

export function PickpointDashboardPage() {
  const { user } = useAuth();
  const qc = useQueryClient();

  const incoming        = usePickpointOrders(["SHIPPED"]);
  const awaiting        = usePickpointOrders(["READY_FOR_PICKUP"]);
  const returnsInProg   = usePickpointOrders(["RETURN_INITIATED"]);
  const customerReturns = usePickpointCustomerReturns("RECEIVED");
  const history         = usePickpointOrders(["DELIVERED", "RETURNED_TO_WAREHOUSE"]);

  const topic = user?.pickpointId
    ? `/topic/pickpoint/${user.pickpointId}`
    : "__disabled__";

  const { connected } = useDeliverySocket({
    topic,
    onMessage: () => {
      qc.invalidateQueries({ queryKey: ["pickpoint-orders"] });
      qc.invalidateQueries({ queryKey: ["pickpoint-customer-returns"] });
    },
  });

  const isLoading =
    incoming.isLoading ||
    awaiting.isLoading ||
    returnsInProg.isLoading ||
    customerReturns.isLoading ||
    history.isLoading;

  const awaitingOrders = awaiting.data?.content ?? [];
  const hasOverdue     = awaitingOrders.some((o) => o.overdue);

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-lg mx-auto px-4 py-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold">
            {user?.pickpointName ?? "My Pickpoint"}
          </h1>
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

        {/* Loading skeleton */}
        {isLoading ? (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-40 w-full rounded-xl" />
            ))}
          </div>
        ) : (
          <Tabs defaultValue="awaiting">
            <TabsList className="w-full grid grid-cols-5">
              <TabsTrigger value="incoming" className="flex-1 text-xs px-1">
                Incoming
                <TabBadge count={incoming.data?.totalElements ?? 0} />
              </TabsTrigger>
              <TabsTrigger value="awaiting" className="flex-1 text-xs px-1">
                Awaiting
                <TabBadge
                  count={awaiting.data?.totalElements ?? 0}
                  tone={hasOverdue ? "amber" : "default"}
                />
              </TabsTrigger>
              <TabsTrigger value="returns" className="flex-1 text-xs px-1">
                Returns
                <TabBadge count={returnsInProg.data?.totalElements ?? 0} />
              </TabsTrigger>
              <TabsTrigger value="customer-returns" className="flex-1 text-xs px-1">
                Walk-in
                <TabBadge count={customerReturns.data?.totalElements ?? 0} />
              </TabsTrigger>
              <TabsTrigger value="history" className="flex-1 text-xs px-1">
                History
              </TabsTrigger>
            </TabsList>

            <TabsContent value="incoming" className="mt-4">
              <OrderList
                orders={incoming.data?.content ?? []}
                emptyMessage="No incoming packages."
                renderCard={(o) => <IncomingPackageCard key={o.orderId} order={o} />}
              />
            </TabsContent>

            <TabsContent value="awaiting" className="mt-4">
              <OrderList
                orders={awaitingOrders}
                emptyMessage="No orders awaiting pickup."
                renderCard={(o) => <PickpointOrderCard key={o.orderId} order={o} />}
              />
            </TabsContent>

            <TabsContent value="returns" className="mt-4">
              <OrderList
                orders={returnsInProg.data?.content ?? []}
                emptyMessage="No returns in progress."
                renderCard={(o) => <ReturnInProgressCard key={o.orderId} order={o} />}
              />
            </TabsContent>

            <TabsContent value="customer-returns" className="mt-4">
              <CustomerReturnsTab />
            </TabsContent>

            <TabsContent value="history" className="mt-4">
              <OrderList
                orders={history.data?.content ?? []}
                emptyMessage="No completed orders."
                renderCard={(o) => <PickpointOrderCard key={o.orderId} order={o} />}
              />
            </TabsContent>
          </Tabs>
        )}
      </div>
    </div>
  );
}
