"use client";

import { useRef, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { ScanLine, ChevronLeft, ChevronRight, PackageSearch } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import { Card, CardContent } from "@/shared/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
import { useDebounce, formatDate } from "@/shared/lib";
import { usePickQueue } from "../api";

export function PickQueuePage() {
  const t = useTranslations("warehouse");
  const router = useRouter();

  const [page, setPage] = useState(1);
  const [searchInput, setSearchInput] = useState("");
  const debouncedSearch = useDebounce(searchInput, 300);

  const jumpRef = useRef<HTMLInputElement>(null);
  const [jumpValue, setJumpValue] = useState("");

  const { data, isLoading } = usePickQueue(page, debouncedSearch);

  useEffect(() => {
    jumpRef.current?.focus();
  }, []);

  function refocusJump() {
    requestAnimationFrame(() => jumpRef.current?.focus());
  }

  function handleJumpSubmit(e: React.FormEvent) {
    e.preventDefault();
    const id = jumpValue.trim();
    if (!id) return;
    router.push(`/warehouse/pick/${id}`);
    setJumpValue("");
    refocusJump();
  }

  function handleSearchChange(value: string) {
    setSearchInput(value);
    setPage(1);
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-zinc-900">{t("pick.queue.title")}</h1>

      {/* Jump bar */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-2 text-amber-600">
              <ScanLine className="h-5 w-5" />
              <span className="text-sm font-semibold">{t("common.scan")}</span>
            </div>
            <form onSubmit={handleJumpSubmit}>
              <Input
                ref={jumpRef}
                value={jumpValue}
                onChange={(e) => setJumpValue(e.target.value)}
                placeholder={t("pick.queue.jumpPrompt")}
                className="text-base h-12 font-mono tracking-wider"
                autoComplete="off"
                autoCorrect="off"
                spellCheck={false}
              />
            </form>
          </div>
        </CardContent>
      </Card>

      {/* Search */}
      <Input
        value={searchInput}
        onChange={(e) => handleSearchChange(e.target.value)}
        placeholder={t("pick.queue.search")}
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
          <p className="text-sm">{t("pick.queue.empty")}</p>
        </div>
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("pick.queue.columns.orderId")}</TableHead>
                <TableHead>{t("pick.queue.columns.deliveryType")}</TableHead>
                <TableHead>{t("pick.queue.columns.customer")}</TableHead>
                <TableHead className="text-right">{t("pick.queue.columns.items")}</TableHead>
                <TableHead>{t("pick.queue.columns.createdAt")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((item) => (
                <TableRow
                  key={item.orderId}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => router.push(`/warehouse/pick/${item.orderId}`)}
                >
                  <TableCell className="font-medium">{item.externalOrderId}</TableCell>
                  <TableCell>
                    <span
                      className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                        item.deliveryType === "HOME"
                          ? "bg-blue-100 text-blue-700"
                          : "bg-orange-100 text-orange-700"
                      }`}
                    >
                      {item.deliveryType}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {item.customerName}
                  </TableCell>
                  <TableCell className="text-right tabular-nums font-medium">
                    {item.pickedUnits} / {item.totalUnits}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground whitespace-nowrap">
                    {formatDate(item.createdAt)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {/* Pagination */}
          {data.totalElements > data.size && (
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>
                {(data.number - 1) * data.size + 1}–
                {Math.min(data.number * data.size, data.totalElements)} /{" "}
                {data.totalElements}
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
