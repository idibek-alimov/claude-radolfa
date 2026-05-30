"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/tabs";
import { Button } from "@/shared/ui/button";
import { DiscountTable } from "./DiscountTable";
import { DiscountTypesPanel } from "./DiscountTypesPanel";
import { DiscountedProductsTable } from "./DiscountedProductsTable";
import { DiscountTimeline } from "./DiscountTimeline";
import { DiscountCalendar } from "./DiscountCalendar";
import type { DiscountResponse } from "../model/types";
import { LayoutGrid, Tag, Package, CalendarRange } from "lucide-react";

const VIEW_STORAGE_KEY = "discount.campaigns.view";
type CampaignView = "table" | "timeline";

export function DiscountsTab() {
  const router = useRouter();
  const [view, setViewState] = useState<CampaignView>("table");
  const [selectedDate, setSelectedDate] = useState<string | null>(null);

  useEffect(() => {
    const stored = localStorage.getItem(VIEW_STORAGE_KEY);
    if (stored === "table" || stored === "timeline") setViewState(stored);
  }, []);

  function setView(v: CampaignView) {
    setViewState(v);
    localStorage.setItem(VIEW_STORAGE_KEY, v);
  }

  const openCreate = () => router.push("/manage/discounts/create");

  const openEdit = (discount: DiscountResponse) =>
    router.push(`/manage/discounts/${discount.id}/edit`);

  const openDuplicate = (discount: DiscountResponse) =>
    router.push(`/manage/discounts/create?from=${discount.id}`);

  return (
    <Tabs defaultValue="campaigns" className="flex flex-col flex-1 min-h-0 gap-6">
      {/* Underline-style sub-tab bar — visually distinct from the outer pill tabs */}
      <div className="border-b shrink-0">
        <TabsList className="h-auto p-0 bg-transparent rounded-none gap-0">
          <TabsTrigger
            value="campaigns"
            className="rounded-none border-b-2 border-transparent data-[state=active]:border-foreground data-[state=active]:bg-transparent data-[state=active]:shadow-none pb-2.5 pt-0.5 px-4 text-sm gap-1.5 -mb-px"
          >
            <LayoutGrid className="h-3.5 w-3.5" />
            Campaigns
          </TabsTrigger>
          <TabsTrigger
            value="types"
            className="rounded-none border-b-2 border-transparent data-[state=active]:border-foreground data-[state=active]:bg-transparent data-[state=active]:shadow-none pb-2.5 pt-0.5 px-4 text-sm gap-1.5 -mb-px"
          >
            <Tag className="h-3.5 w-3.5" />
            Types
          </TabsTrigger>
          <TabsTrigger
            value="products"
            className="rounded-none border-b-2 border-transparent data-[state=active]:border-foreground data-[state=active]:bg-transparent data-[state=active]:shadow-none pb-2.5 pt-0.5 px-4 text-sm gap-1.5 -mb-px"
          >
            <Package className="h-3.5 w-3.5" />
            Discounted Products
          </TabsTrigger>
        </TabsList>
      </div>

      <TabsContent value="campaigns" className="mt-0 flex-1 min-h-0 flex flex-col">
        <div className="flex flex-col flex-1 min-h-0 gap-3">
          {/* Calendar — shown only in table view */}
          {view === "table" && (
            <DiscountCalendar
              selectedDate={selectedDate}
              onDayClick={setSelectedDate}
            />
          )}

          {/* View toggle */}
          <div className="flex items-center gap-1 border rounded-lg p-0.5 bg-muted/40 w-fit shrink-0">
            <Button
              variant={view === "table" ? "default" : "ghost"}
              size="sm"
              className="h-7 px-3 gap-1.5 text-xs"
              onClick={() => setView("table")}
            >
              <LayoutGrid className="h-3.5 w-3.5" />
              Table
            </Button>
            <Button
              variant={view === "timeline" ? "default" : "ghost"}
              size="sm"
              className="h-7 px-3 gap-1.5 text-xs"
              onClick={() => setView("timeline")}
            >
              <CalendarRange className="h-3.5 w-3.5" />
              Timeline
            </Button>
          </div>

          {view === "table" ? (
            <DiscountTable
              onEdit={openEdit}
              onNew={openCreate}
              onDuplicate={openDuplicate}
              externalDateFilter={selectedDate}
              onClearDateFilter={() => setSelectedDate(null)}
            />
          ) : (
            <DiscountTimeline onEdit={openEdit} />
          )}
        </div>
      </TabsContent>

      <TabsContent value="types" className="mt-0 flex-1 min-h-0 flex flex-col overflow-hidden">
        <div className="flex flex-col flex-1 min-h-0">
          <DiscountTypesPanel />
        </div>
      </TabsContent>

      <TabsContent value="products" className="mt-0 flex-1 min-h-0 flex flex-col overflow-hidden">
        <div className="flex flex-col flex-1 min-h-0">
          <DiscountedProductsTable />
        </div>
      </TabsContent>
    </Tabs>
  );
}
