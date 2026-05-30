"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { PackagePlus, PackageSearch } from "lucide-react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
import { useDebounce, formatDate } from "@/shared/lib";
import { useStockReceipts } from "../api";

export function StockReceiptListPage() {
  const t = useTranslations("warehouse");
  const router = useRouter();
  const [page, setPage] = useState(1);
  const [searchInput, setSearchInput] = useState("");
  const debouncedSearch = useDebounce(searchInput, 300);
  const { data, isLoading } = useStockReceipts(page, debouncedSearch);

  function handleSearchChange(value: string) {
    setSearchInput(value);
    setPage(1);
  }

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-zinc-900">{t("receipts.title")}</h1>
        <Button onClick={() => router.push("/warehouse/receipts/new")}>
          <PackagePlus className="h-4 w-4 mr-2" />
          {t("receipts.new")}
        </Button>
      </div>

      {/* Search */}
      <Input
        value={searchInput}
        onChange={(e) => handleSearchChange(e.target.value)}
        placeholder={t("receipts.search")}
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
          <PackageSearch className="h-10 w-10 text-muted-foreground/40" />
          <p className="text-sm">{t("receipts.empty")}</p>
          <Button variant="outline" size="sm" onClick={() => router.push("/warehouse/receipts/new")}>
            {t("receipts.emptyCta")}
          </Button>
        </div>
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("receipts.columns.date")}</TableHead>
                <TableHead>{t("receipts.columns.supplier")}</TableHead>
                <TableHead className="text-right">{t("receipts.columns.units")}</TableHead>
                <TableHead>{t("receipts.columns.createdBy")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((receipt) => (
                <TableRow
                  key={receipt.id}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => router.push(`/warehouse/receipts/${receipt.id}`)}
                >
                  <TableCell className="text-sm text-muted-foreground whitespace-nowrap">
                    {formatDate(receipt.createdAt)}
                  </TableCell>
                  <TableCell className="font-medium">
                    {receipt.supplierReference ?? <span className="text-muted-foreground italic">—</span>}
                  </TableCell>
                  <TableCell className="text-right font-bold tabular-nums">
                    {receipt.totalUnitsReceived}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    #{receipt.createdByUserId}
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
    </div>
  );
}
