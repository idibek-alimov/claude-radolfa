"use client";

import { useState, useMemo, useCallback, useRef } from "react";
import { useMutation, useQuery, useQueryClient, keepPreviousData } from "@tanstack/react-query";
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/shared/ui/table";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { fetchDiscounts, fetchDiscountTypes, enableDiscount, disableDiscount } from "../api";
import type { DiscountListFilters, DiscountResponse } from "../model/types";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import {
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  ChevronDown,
  ChevronsUpDown,
  Plus,
  MoreHorizontal,
  Pencil,
  Copy,
  PowerOff,
  Power,
  Search,
  Clock,
  CheckCircle2,
  XCircle,
  Calendar,
} from "lucide-react";
import { cn } from "@/shared/lib";

const PAGE_SIZE = 15;

// ── Status derivation ─────────────────────────────────────────────

type DerivedStatus = "active" | "scheduled" | "expired" | "disabled";

const STATUS_ORDER: Record<DerivedStatus, number> = {
  active: 0,
  scheduled: 1,
  expired: 2,
  disabled: 3,
};

function getStatus(d: DiscountResponse): DerivedStatus {
  if (d.disabled) return "disabled";
  const now = new Date();
  if (new Date(d.validUpto) < now) return "expired";
  if (new Date(d.validFrom) > now) return "scheduled";
  return "active";
}

// ── Sort ──────────────────────────────────────────────────────────

type SortKey =
  | "title"
  | "type"
  | "discountValue"
  | "validFrom"
  | "validUpto"
  | "itemCodes"
  | "status";

interface Sort {
  key: SortKey;
  dir: "asc" | "desc";
}

function sortItems(items: DiscountResponse[], sort: Sort | null): DiscountResponse[] {
  if (!sort) return items;
  const { key, dir } = sort;
  const mul = dir === "asc" ? 1 : -1;
  return [...items].sort((a, b) => {
    let cmp = 0;
    switch (key) {
      case "title":
        cmp = a.title.localeCompare(b.title);
        break;
      case "type":
        cmp = a.type.name.localeCompare(b.type.name);
        break;
      case "discountValue":
        cmp = a.discountValue - b.discountValue;
        break;
      case "validFrom":
        cmp = new Date(a.validFrom).getTime() - new Date(b.validFrom).getTime();
        break;
      case "validUpto":
        cmp = new Date(a.validUpto).getTime() - new Date(b.validUpto).getTime();
        break;
      case "itemCodes":
        cmp = a.itemCodes.length - b.itemCodes.length;
        break;
      case "status":
        cmp = STATUS_ORDER[getStatus(a)] - STATUS_ORDER[getStatus(b)];
        break;
    }
    return cmp * mul;
  });
}

// ── SortableHeader ────────────────────────────────────────────────

function SortableHeader({
  label,
  sortKey,
  sort,
  onSort,
}: {
  label: string;
  sortKey: SortKey;
  sort: Sort | null;
  onSort: (key: SortKey) => void;
}) {
  const isActive = sort?.key === sortKey;
  const Icon = isActive
    ? sort!.dir === "asc"
      ? ChevronUp
      : ChevronDown
    : ChevronsUpDown;

  return (
    <button
      className="inline-flex items-center gap-1 group/sort whitespace-nowrap"
      onClick={() => onSort(sortKey)}
    >
      <span
        className={cn(
          "text-xs font-medium transition-colors",
          isActive
            ? "text-foreground"
            : "text-muted-foreground group-hover/sort:text-foreground"
        )}
      >
        {label}
      </span>
      <Icon
        className={cn(
          "h-3 w-3 transition-colors",
          isActive
            ? "text-foreground"
            : "text-muted-foreground group-hover/sort:text-foreground"
        )}
      />
    </button>
  );
}

// ── StatusBadge ───────────────────────────────────────────────────

const STATUS_CONFIG: Record<
  DerivedStatus,
  { label: string; dotClass: string; textClass: string; Icon: React.ElementType }
> = {
  active: {
    label: "Active",
    dotClass: "bg-green-500",
    textClass: "text-green-600 dark:text-green-400",
    Icon: CheckCircle2,
  },
  scheduled: {
    label: "Scheduled",
    dotClass: "bg-blue-500",
    textClass: "text-blue-600 dark:text-blue-400",
    Icon: Calendar,
  },
  expired: {
    label: "Expired",
    dotClass: "bg-orange-500",
    textClass: "text-orange-600 dark:text-orange-400",
    Icon: Clock,
  },
  disabled: {
    label: "Disabled",
    dotClass: "bg-muted-foreground/50",
    textClass: "text-muted-foreground",
    Icon: XCircle,
  },
};

function StatusBadge({ discount }: { discount: DiscountResponse }) {
  const status = getStatus(discount);
  const { label, dotClass, textClass } = STATUS_CONFIG[status];
  return (
    <span className={cn("inline-flex items-center gap-1.5 text-xs font-medium", textClass)}>
      <span className={cn("h-1.5 w-1.5 rounded-full shrink-0", dotClass)} />
      {label}
    </span>
  );
}

// ── SkuCodesPopover ───────────────────────────────────────────────

function SkuCodesPopover({ codes }: { codes: string[] }) {
  const [open, setOpen] = useState(false);
  const preview = codes.slice(0, 5);
  const remaining = codes.length - preview.length;

  return (
    <div className="relative">
      <button
        className="text-sm text-muted-foreground hover:text-foreground underline-offset-2 hover:underline transition-colors tabular-nums"
        onClick={() => setOpen((v) => !v)}
      >
        {codes.length} SKU{codes.length !== 1 ? "s" : ""}
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute z-20 left-0 top-full mt-1 min-w-[180px] rounded-lg border border-border bg-popover shadow-md p-3 space-y-1">
            <p className="text-xs font-semibold text-muted-foreground mb-2">SKU codes</p>
            {preview.map((code) => (
              <p key={code} className="text-xs font-mono text-foreground">
                {code}
              </p>
            ))}
            {remaining > 0 && (
              <p className="text-xs text-muted-foreground">+{remaining} more…</p>
            )}
          </div>
        </>
      )}
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────

interface DiscountTableProps {
  onEdit: (discount: DiscountResponse) => void;
  onNew: () => void;
  onDuplicate: (discount: DiscountResponse) => void;
}

export function DiscountTable({ onEdit, onNew, onDuplicate }: DiscountTableProps) {
  const [filters, setFilters] = useState<DiscountListFilters>({
    page: 1,
    size: PAGE_SIZE,
    status: "all",
  });
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [sort, setSort] = useState<Sort | null>(null);
  const debounceRef = useRef<NodeJS.Timeout | undefined>(undefined);

  const qc = useQueryClient();

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim());
      setFilters((f) => ({ ...f, page: 1 }));
    }, 250);
  }, []);

  const handleSort = useCallback((key: SortKey) => {
    setSort((prev) =>
      prev?.key === key
        ? { key, dir: prev.dir === "asc" ? "desc" : "asc" }
        : { key, dir: "asc" }
    );
  }, []);

  const { data: types = [] } = useQuery({
    queryKey: ["discount-types"],
    queryFn: fetchDiscountTypes,
  });

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["discounts", filters],
    queryFn: () => fetchDiscounts(filters),
    placeholderData: keepPreviousData,
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, disabled }: { id: number; disabled: boolean }) =>
      disabled ? enableDiscount(id) : disableDiscount(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["discounts"] }),
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleDateString(undefined, {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });

  const getDuration = (from: string, to: string): string => {
    const days = Math.round(
      (new Date(to).getTime() - new Date(from).getTime()) / 86_400_000
    );
    return days > 0 ? `${days}d` : "—";
  };

  const visible = useMemo(() => {
    const items = data?.content ?? [];
    const filtered = debouncedSearch
      ? items.filter((d) =>
          d.title.toLowerCase().includes(debouncedSearch.toLowerCase())
        )
      : items;
    return sortItems(filtered, sort);
  }, [data?.content, debouncedSearch, sort]);

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex flex-wrap items-end gap-3">
        {/* Text search */}
        <div className="relative min-w-[180px] flex-1 max-w-xs">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
          <Input
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder="Search campaigns…"
            className="pl-8 h-9 text-sm"
          />
        </div>

        {/* Type filter */}
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Type</label>
          <Select
            value={filters.typeId?.toString() ?? "all"}
            onValueChange={(v) =>
              setFilters((f) => ({
                ...f,
                page: 1,
                typeId: v === "all" ? undefined : Number(v),
              }))
            }
          >
            <SelectTrigger className="h-9 w-[160px] text-sm">
              <SelectValue placeholder="All types" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All types</SelectItem>
              {types.map((t) => (
                <SelectItem key={t.id} value={t.id.toString()}>
                  {t.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Status filter */}
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Status</label>
          <Select
            value={filters.status ?? "all"}
            onValueChange={(v) =>
              setFilters((f) => ({
                ...f,
                page: 1,
                status: v as DiscountListFilters["status"],
              }))
            }
          >
            <SelectTrigger className="h-9 w-[148px] text-sm">
              <SelectValue placeholder="All statuses" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All statuses</SelectItem>
              <SelectItem value="active">Active</SelectItem>
              <SelectItem value="disabled">Disabled</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <Button size="sm" className="ml-auto gap-1.5" onClick={onNew}>
          <Plus className="h-3.5 w-3.5" />
          New Campaign
        </Button>
      </div>

      {/* Table */}
      <div
        className={cn(
          "bg-card rounded-xl border shadow-sm transition-opacity overflow-x-auto",
          isFetching && !isLoading && "opacity-60"
        )}
      >
        {isLoading ? (
          <div className="p-6 space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4 min-w-[180px]">
                  <SortableHeader label="Campaign" sortKey="title" sort={sort} onSort={handleSort} />
                </TableHead>
                <TableHead className="min-w-[120px]">
                  <SortableHeader label="Type" sortKey="type" sort={sort} onSort={handleSort} />
                </TableHead>
                <TableHead className="min-w-[90px]">
                  <SortableHeader label="Discount" sortKey="discountValue" sort={sort} onSort={handleSort} />
                </TableHead>
                <TableHead className="min-w-[110px]">
                  <SortableHeader label="Valid from" sortKey="validFrom" sort={sort} onSort={handleSort} />
                </TableHead>
                <TableHead className="min-w-[110px]">
                  <SortableHeader label="Valid until" sortKey="validUpto" sort={sort} onSort={handleSort} />
                </TableHead>
                <TableHead className="min-w-[80px] text-xs font-medium text-muted-foreground whitespace-nowrap">
                  Duration
                </TableHead>
                <TableHead className="min-w-[80px]">
                  <SortableHeader label="SKUs" sortKey="itemCodes" sort={sort} onSort={handleSort} />
                </TableHead>
                <TableHead className="min-w-[100px]">
                  <SortableHeader label="Status" sortKey="status" sort={sort} onSort={handleSort} />
                </TableHead>
                <TableHead className="pr-4 min-w-[52px]" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {visible.map((d) => (
                <TableRow key={d.id}>
                  {/* Campaign — colored title pill */}
                  <TableCell className="pl-4">
                    <span
                      className="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold text-white max-w-[180px] truncate"
                      style={{ backgroundColor: `#${d.colorHex}` }}
                    >
                      {d.title}
                    </span>
                  </TableCell>

                  {/* Type — muted chip */}
                  <TableCell>
                    <span className="inline-block rounded-md bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground whitespace-nowrap">
                      {d.type.name}
                    </span>
                  </TableCell>

                  {/* Discount % */}
                  <TableCell>
                    <span className="text-sm font-bold tabular-nums text-rose-600 dark:text-rose-400">
                      −{d.discountValue}%
                    </span>
                  </TableCell>

                  {/* Valid from */}
                  <TableCell className="text-sm text-muted-foreground whitespace-nowrap tabular-nums">
                    {formatDate(d.validFrom)}
                  </TableCell>

                  {/* Valid until */}
                  <TableCell className="text-sm text-muted-foreground whitespace-nowrap tabular-nums">
                    {formatDate(d.validUpto)}
                  </TableCell>

                  {/* Duration */}
                  <TableCell className="text-sm text-muted-foreground tabular-nums">
                    {getDuration(d.validFrom, d.validUpto)}
                  </TableCell>

                  {/* SKUs */}
                  <TableCell>
                    <SkuCodesPopover codes={d.itemCodes} />
                  </TableCell>

                  {/* Status */}
                  <TableCell>
                    <StatusBadge discount={d} />
                  </TableCell>

                  {/* Actions */}
                  <TableCell className="text-right pr-4">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button size="sm" variant="ghost" className="h-8 w-8 p-0">
                          <MoreHorizontal className="h-4 w-4" />
                          <span className="sr-only">Actions</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => onEdit(d)}>
                          <Pencil className="h-3.5 w-3.5 mr-2" />
                          Edit
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onDuplicate(d)}>
                          <Copy className="h-3.5 w-3.5 mr-2" />
                          Duplicate
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                          onClick={() =>
                            toggleMutation.mutate({ id: d.id, disabled: d.disabled })
                          }
                          disabled={toggleMutation.isPending}
                          className={d.disabled ? "text-green-600" : "text-muted-foreground"}
                        >
                          {d.disabled ? (
                            <>
                              <Power className="h-3.5 w-3.5 mr-2" />
                              Enable
                            </>
                          ) : (
                            <>
                              <PowerOff className="h-3.5 w-3.5 mr-2" />
                              Disable
                            </>
                          )}
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {!isLoading && visible.length === 0 && (
          <div className="p-12 text-center text-muted-foreground">
            {debouncedSearch
              ? `No campaigns matching "${debouncedSearch}".`
              : "No discounts found."}
          </div>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            {data.totalElements} campaign{data.totalElements !== 1 ? "s" : ""} total
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={filters.page <= 1}
              onClick={() => setFilters((f) => ({ ...f, page: f.page - 1 }))}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="text-sm text-muted-foreground">
              {filters.page} / {data.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
              onClick={() => setFilters((f) => ({ ...f, page: f.page + 1 }))}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
