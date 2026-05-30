"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Forklift } from "lucide-react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@radolfa/shared/ui/button";
import { Input } from "@radolfa/shared/ui/input";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@radolfa/shared/ui/table";
import { useDebounce } from "@radolfa/shared/lib";
import { useInboundQueue } from "../api";
import type { InboundQueueItem } from "../types";
import { PutawayDialog } from "./PutawayDialog";

export function PutawayPage() {
  const t = useTranslations("warehouse");
  const [page, setPage] = useState(1);
  const [searchInput, setSearchInput] = useState("");
  const debouncedSearch = useDebounce(searchInput, 300);
  const { data, isLoading } = useInboundQueue(page, debouncedSearch);
  const [selected, setSelected] = useState<InboundQueueItem | null>(null);

  function handleSearchChange(value: string) {
    setSearchInput(value);
    setPage(1);
  }

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-zinc-900">{t("putaway.title")}</h1>
      </div>

      {/* Search */}
      <Input
        value={searchInput}
        onChange={(e) => handleSearchChange(e.target.value)}
        placeholder={t("putaway.search")}
        className="max-w-sm"
      />

      {/* Table */}
      {isLoading ? (
        <div className="space-y-2">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-lg" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center border border-dashed rounded-xl p-12 text-muted-foreground gap-3">
          <Forklift className="h-10 w-10 text-muted-foreground/40" />
          <p className="text-sm">{t("putaway.empty")}</p>
        </div>
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("putaway.columns.sku")}</TableHead>
                <TableHead>{t("putaway.columns.product")}</TableHead>
                <TableHead>{t("putaway.columns.barcode")}</TableHead>
                <TableHead className="text-right">{t("putaway.columns.unassigned")}</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((item) => (
                <TableRow key={item.skuId}>
                  <TableCell className="font-mono text-sm">{item.skuCode}</TableCell>
                  <TableCell className="font-medium">{item.productName}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">{item.barcode}</TableCell>
                  <TableCell className="text-right font-bold tabular-nums">
                    {item.unassignedQuantity}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setSelected(item)}
                    >
                      {t("putaway.action")}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {/* Pagination */}
          {data.totalElements > data.size && (
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>
                {(data.number - 1) * data.size + 1}–{Math.min(data.number * data.size, data.totalElements)}{" "}
                / {data.totalElements}
              </span>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => p - 1)}
                  disabled={page <= 1}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => p + 1)}
                  disabled={data.last}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      <PutawayDialog item={selected} onClose={() => setSelected(null)} />
    </div>
  );
}
