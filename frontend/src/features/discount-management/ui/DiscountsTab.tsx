"use client";

import { useRouter } from "next/navigation";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/tabs";
import { DiscountTable } from "./DiscountTable";
import { DiscountTypesPanel } from "./DiscountTypesPanel";
import { DiscountedProductsTable } from "./DiscountedProductsTable";
import type { DiscountResponse } from "../model/types";
import { LayoutGrid, Tag, Package } from "lucide-react";

export function DiscountsTab() {
  const router = useRouter();

  const openCreate = () => router.push("/manage/discounts/create");

  const openEdit = (discount: DiscountResponse) =>
    router.push(`/manage/discounts/${discount.id}/edit`);

  const openDuplicate = (discount: DiscountResponse) =>
    router.push(`/manage/discounts/create?from=${discount.id}`);

  return (
    <Tabs defaultValue="campaigns" className="flex flex-col flex-1 min-h-0 gap-6">
      {/* Underline-style sub-tab bar — visually distinct from the outer pill tabs */}
      <div className="border-b">
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

      <TabsContent value="campaigns" className="mt-0 flex flex-col flex-1 min-h-0">
        <DiscountTable
          onEdit={openEdit}
          onNew={openCreate}
          onDuplicate={openDuplicate}
        />
      </TabsContent>

      <TabsContent value="types" className="mt-0">
        <DiscountTypesPanel />
      </TabsContent>

      <TabsContent value="products" className="mt-0 flex flex-col flex-1 min-h-0">
        <DiscountedProductsTable />
      </TabsContent>
    </Tabs>
  );
}
