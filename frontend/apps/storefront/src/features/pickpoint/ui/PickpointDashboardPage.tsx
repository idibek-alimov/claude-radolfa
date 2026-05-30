"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { cn } from "@radolfa/shared/lib/utils";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { Button } from "@radolfa/shared/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@radolfa/shared/ui/tabs";
import { useAuth } from "@radolfa/shared/auth";
import {
  usePickpointOrders,
  usePickpointCustomerReturns,
  useConfirmArrival,
} from "@/features/pickpoint/api";
import { useDeliverySocket } from "@radolfa/shared/lib/useDeliverySocket";
import { getErrorMessage } from "@radolfa/shared/lib";
import type { PickpointOrder } from "@radolfa/shared/user";
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
  const t = useTranslations("pickpoint");
  const confirmArrival = useConfirmArrival();
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  const toggleOrder = (id: number) =>
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

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
          <Tabs
            defaultValue="awaiting"
            onValueChange={() => setSelectedIds(new Set())}
          >
            <TabsList className="flex w-full gap-2 overflow-x-auto pb-1 scrollbar-hide bg-transparent p-0 h-auto">
              <TabsTrigger
                value="incoming"
                className="shrink-0 whitespace-nowrap rounded-full border px-3 py-1.5 text-xs data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
              >
                {t("tab.incoming")}
                <TabBadge count={incoming.data?.totalElements ?? 0} />
              </TabsTrigger>
              <TabsTrigger
                value="awaiting"
                className="shrink-0 whitespace-nowrap rounded-full border px-3 py-1.5 text-xs data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
              >
                {t("tab.awaiting")}
                <TabBadge
                  count={awaiting.data?.totalElements ?? 0}
                  tone={hasOverdue ? "amber" : "default"}
                />
              </TabsTrigger>
              <TabsTrigger
                value="returns"
                className="shrink-0 whitespace-nowrap rounded-full border px-3 py-1.5 text-xs data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
              >
                {t("tab.returns")}
                <TabBadge count={returnsInProg.data?.totalElements ?? 0} />
              </TabsTrigger>
              <TabsTrigger
                value="customer-returns"
                className="shrink-0 whitespace-nowrap rounded-full border px-3 py-1.5 text-xs data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
              >
                {t("tab.customerReturns")}
                <TabBadge count={customerReturns.data?.totalElements ?? 0} />
              </TabsTrigger>
              <TabsTrigger
                value="history"
                className="shrink-0 whitespace-nowrap rounded-full border px-3 py-1.5 text-xs data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
              >
                {t("tab.history")}
              </TabsTrigger>
            </TabsList>

            <TabsContent value="incoming" className="mt-4">
              {(incoming.data?.content ?? []).length === 0 ? (
                <EmptyTab message={t("noOrders")} />
              ) : (
                <div className="space-y-4">
                  {(incoming.data?.content ?? []).map((o) => (
                    <IncomingPackageCard
                      key={o.orderId}
                      order={o}
                      selected={selectedIds.has(o.orderId)}
                      onToggle={toggleOrder}
                    />
                  ))}
                </div>
              )}
            </TabsContent>

            <TabsContent value="awaiting" className="mt-4">
              <OrderList
                orders={awaitingOrders}
                emptyMessage={t("noOrders")}
                renderCard={(o) => <PickpointOrderCard key={o.orderId} order={o} />}
              />
            </TabsContent>

            <TabsContent value="returns" className="mt-4">
              <OrderList
                orders={returnsInProg.data?.content ?? []}
                emptyMessage={t("noOrders")}
                renderCard={(o) => <ReturnInProgressCard key={o.orderId} order={o} />}
              />
            </TabsContent>

            <TabsContent value="customer-returns" className="mt-4">
              <CustomerReturnsTab />
            </TabsContent>

            <TabsContent value="history" className="mt-4">
              <OrderList
                orders={history.data?.content ?? []}
                emptyMessage={t("noOrders")}
                renderCard={(o) => <PickpointOrderCard key={o.orderId} order={o} />}
              />
            </TabsContent>
          </Tabs>
        )}

        {selectedIds.size > 0 && (
          <div className="fixed bottom-0 left-0 right-0 z-40 border-t bg-background p-3 shadow-lg">
            <div className="mx-auto flex max-w-lg items-center justify-between">
              <span className="text-sm text-muted-foreground">
                {t("selectedCount", { count: selectedIds.size })}
              </span>
              <Button
                size="sm"
                disabled={confirmArrival.isPending}
                onClick={async () => {
                  try {
                    await Promise.all(
                      [...selectedIds].map((id) =>
                        confirmArrival.mutateAsync(id),
                      ),
                    );
                    toast.success(
                      t("confirmedToast", { count: selectedIds.size }),
                    );
                    setSelectedIds(new Set());
                  } catch (err) {
                    toast.error(getErrorMessage(err));
                  }
                }}
              >
                {confirmArrival.isPending
                  ? t("confirming")
                  : t("confirmSelected", { count: selectedIds.size })}
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
