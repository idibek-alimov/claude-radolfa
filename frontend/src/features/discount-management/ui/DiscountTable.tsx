"use client";

import { useState, useCallback, useRef, useEffect, useMemo } from "react";
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
import { Checkbox } from "@/shared/ui/checkbox";
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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/ui/alert-dialog";
import {
  fetchDiscounts,
  fetchDiscountTypes,
  fetchDiscountOverlaps,
  enableDiscount,
  disableDiscount,
  bulkEnableDiscounts,
  bulkDisableDiscounts,
  bulkDeleteDiscounts,
  bulkDuplicateDiscounts,
} from "../api";
import type { DiscountListFilters, DiscountResponse, DiscountOverlapRow } from "../model/types";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/shared/ui/tooltip";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import {
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
  Trash2,
  AlertTriangle,
  X,
  List,
  Tag,
  Users,
  Ticket,
} from "lucide-react";
import { cn } from "@/shared/lib";
import { CampaignSkuDrawer } from "./CampaignSkuDrawer";


// ── Status derivation (badge rendering only — no filtering) ───────────────────

type DerivedStatus = "active" | "scheduled" | "expired" | "disabled";

function getStatus(d: DiscountResponse): DerivedStatus {
  if (d.disabled) return "disabled";
  const now = new Date();
  if (new Date(d.validUpto) < now) return "expired";
  if (new Date(d.validFrom) > now) return "scheduled";
  return "active";
}

// ── Sort (server-side keys only — title, amountValue, validFrom, validUpto) ──

type SortKey = "title" | "amountValue" | "validFrom" | "validUpto";

interface Sort {
  key: SortKey;
  dir: "asc" | "desc";
}

// ── SortableHeader ────────────────────────────────────────────────────────────

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

// ── StatusBadge ───────────────────────────────────────────────────────────────

const STATUS_CONFIG: Record<
  DerivedStatus,
  { label: string; dotClass: string; textClass: string }
> = {
  active: {
    label: "Active",
    dotClass: "bg-green-500",
    textClass: "text-green-600 dark:text-green-400",
  },
  scheduled: {
    label: "Scheduled",
    dotClass: "bg-blue-500",
    textClass: "text-blue-600 dark:text-blue-400",
  },
  expired: {
    label: "Expired",
    dotClass: "bg-orange-500",
    textClass: "text-orange-600 dark:text-orange-400",
  },
  disabled: {
    label: "Disabled",
    dotClass: "bg-muted-foreground/50",
    textClass: "text-muted-foreground",
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

// ── Main component ────────────────────────────────────────────────────────────

interface DiscountTableProps {
  onEdit: (discount: DiscountResponse) => void;
  onNew: () => void;
  onDuplicate: (discount: DiscountResponse) => void;
  externalDateFilter?: string | null; // "YYYY-MM-DD" — scopes list to that day
  onClearDateFilter?: () => void;
}

export function DiscountTable({ onEdit, onNew, onDuplicate, externalDateFilter, onClearDateFilter }: DiscountTableProps) {
  const [filters, setFilters] = useState<DiscountListFilters>({
    page: 1,
    size: 200,
    status: "all",
  });
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [sort, setSort] = useState<Sort | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [drawerCampaignId, setDrawerCampaignId] = useState<number | null>(null);
  const debounceRef = useRef<NodeJS.Timeout | undefined>(undefined);

  // Clear selection on any filter/search/sort change to avoid stale IDs
  useEffect(() => {
    setSelectedIds(new Set());
  }, [filters, debouncedSearch, sort]);

  const qc = useQueryClient();

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim());
      setFilters((f) => ({ ...f, page: 1 }));
    }, 300);
  }, []);

  const handleSort = useCallback((key: SortKey) => {
    setSort((prev) =>
      prev?.key === key
        ? { key, dir: prev.dir === "asc" ? "desc" : "asc" }
        : { key, dir: "asc" }
    );
    setFilters((f) => ({ ...f, page: 1 }));
  }, []);

  const sortParam = sort ? `${sort.key},${sort.dir}` : undefined;

  const { data: types = [] } = useQuery({
    queryKey: ["discount-types"],
    queryFn: fetchDiscountTypes,
  });

  // When an external date is selected by the calendar, scope the query to that day
  const externalFrom = externalDateFilter ? `${externalDateFilter}T00:00:00.000Z` : undefined;
  const externalTo = externalDateFilter
    ? new Date(new Date(externalFrom!).getTime() + 86_400_000).toISOString()
    : undefined;

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["discounts", filters, debouncedSearch, sortParam, externalDateFilter],
    queryFn: () =>
      fetchDiscounts({
        ...filters,
        search: debouncedSearch || undefined,
        sort: sortParam,
        from: externalFrom ?? filters.from,
        to: externalTo ?? filters.to,
      }),
    placeholderData: keepPreviousData,
  });

  const { data: overlaps } = useQuery({
    queryKey: ["discount-overlaps"],
    queryFn: fetchDiscountOverlaps,
    staleTime: 60_000,
  });

  // Map campaignId → list of { skuCode, winnerTitle } for the Conflicts column
  const overlapMap = useMemo(() => {
    const map = new Map<number, { skuCode: string; winnerTitle: string }[]>();
    if (!overlaps) return map;
    for (const row of overlaps) {
      const entry = { skuCode: row.skuCode, winnerTitle: row.winningCampaign.title };
      [row.winningCampaign, ...row.losingCampaigns].forEach((c) => {
        if (!map.has(c.id)) map.set(c.id, []);
        map.get(c.id)!.push(entry);
      });
    }
    return map;
  }, [overlaps]);

  const rows = data?.content ?? [];

  const toggleMutation = useMutation({
    mutationFn: ({ id, disabled }: { id: number; disabled: boolean }) =>
      disabled ? enableDiscount(id) : disableDiscount(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["discounts"] });
      qc.invalidateQueries({ queryKey: ["discount-overlaps"] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // ── Selection helpers ─────────────────────────────────────────────

  const allSelected = rows.length > 0 && rows.every((r) => selectedIds.has(r.id));
  const someSelected = !allSelected && rows.some((r) => selectedIds.has(r.id));

  const toggleAll = () => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (allSelected) {
        rows.forEach((r) => next.delete(r.id));
      } else {
        rows.forEach((r) => next.add(r.id));
      }
      return next;
    });
  };

  const toggleRow = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  // ── Bulk action handlers ──────────────────────────────────────────

  const selectedList = Array.from(selectedIds);

  const invalidateAll = () => {
    qc.invalidateQueries({ queryKey: ["discounts"] });
    qc.invalidateQueries({ queryKey: ["discount-overlaps"] });
  };

  const handleBulkEnable = async () => {
    try {
      const { affected } = await bulkEnableDiscounts(selectedList);
      toast.success(`Enabled ${affected} campaign${affected !== 1 ? "s" : ""}`);
      invalidateAll();
      setSelectedIds(new Set());
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const handleBulkDisable = async () => {
    try {
      const { affected } = await bulkDisableDiscounts(selectedList);
      toast.success(`Disabled ${affected} campaign${affected !== 1 ? "s" : ""}`);
      invalidateAll();
      setSelectedIds(new Set());
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const handleBulkDelete = async () => {
    try {
      const { affected } = await bulkDeleteDiscounts(selectedList);
      toast.success(`Deleted ${affected} campaign${affected !== 1 ? "s" : ""}`);
      invalidateAll();
      setSelectedIds(new Set());
      setDeleteDialogOpen(false);
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const handleBulkDuplicate = async () => {
    try {
      const result = await bulkDuplicateDiscounts(selectedList);
      toast.success(`Duplicated ${result.length} campaign${result.length !== 1 ? "s" : ""}`);
      invalidateAll();
      setSelectedIds(new Set());
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  // Titles of selected rows on the current page (for delete confirmation)
  const selectedTitles = rows.filter((r) => selectedIds.has(r.id)).map((r) => r.title);
  const deletePreview =
    selectedTitles.slice(0, 3).map((t) => `"${t}"`).join(", ") +
    (selectedTitles.length > 3 ? ` +${selectedTitles.length - 3} more` : "");

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

  return (
    <TooltipProvider delayDuration={200}>
    <div className="flex flex-col flex-1 min-h-0 gap-4">
      {/* External date filter chip */}
      {externalDateFilter && (
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-full border bg-primary/5 px-3 py-1 text-xs font-medium text-primary">
            Filtering by{" "}
            {new Date(externalDateFilter + "T12:00:00").toLocaleDateString(undefined, {
              day: "2-digit",
              month: "short",
              year: "numeric",
            })}
            <button
              onClick={onClearDateFilter}
              className="ml-1 rounded-full hover:bg-primary/20 p-0.5 transition-colors"
              aria-label="Clear date filter"
            >
              <X className="h-3 w-3" />
            </button>
          </span>
        </div>
      )}

      {/* Filters toolbar */}
      <div className="flex flex-wrap items-end gap-3">
        {/* Text search — sent server-side after 300 ms debounce */}
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

        {/* Status filter — all four statuses resolved server-side */}
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
              <SelectItem value="scheduled">Scheduled</SelectItem>
              <SelectItem value="expired">Expired</SelectItem>
              <SelectItem value="disabled">Disabled</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <Button size="sm" className="ml-auto gap-1.5" onClick={onNew}>
          <Plus className="h-3.5 w-3.5" />
          New Campaign
        </Button>
      </div>

      {/* Bulk actions toolbar — visible only when rows are selected */}
      {selectedIds.size > 0 && (
        <div className="flex items-center justify-between rounded-lg border border-border bg-muted/40 px-4 py-2">
          <span className="text-sm text-muted-foreground">
            {selectedIds.size} selected
          </span>
          <div className="flex items-center gap-2">
            <Button size="sm" variant="outline" onClick={handleBulkEnable}>
              <Power className="h-3.5 w-3.5 mr-1.5" />
              Enable
            </Button>
            <Button size="sm" variant="outline" onClick={handleBulkDisable}>
              <PowerOff className="h-3.5 w-3.5 mr-1.5" />
              Disable
            </Button>
            <Button size="sm" variant="outline" onClick={handleBulkDuplicate}>
              <Copy className="h-3.5 w-3.5 mr-1.5" />
              Duplicate
            </Button>
            <Button
              size="sm"
              variant="destructive"
              onClick={() => setDeleteDialogOpen(true)}
            >
              <Trash2 className="h-3.5 w-3.5 mr-1.5" />
              Delete
            </Button>
          </div>
        </div>
      )}

      {/* Bulk delete confirmation dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              Delete {selectedIds.size} campaign{selectedIds.size !== 1 ? "s" : ""}?
            </AlertDialogTitle>
            <AlertDialogDescription>
              This cannot be undone. Campaigns: {deletePreview}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={handleBulkDelete}
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Table */}
      <div
        className={cn(
          "flex-1 min-h-0 overflow-auto bg-card rounded-xl border shadow-sm transition-opacity",
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
            <TableHeader className="sticky top-0 z-10 bg-card">
              <TableRow className="hover:bg-transparent">
                {/* Master select checkbox */}
                <TableHead className="pl-4 w-10">
                  <Checkbox
                    checked={allSelected ? true : someSelected ? "indeterminate" : false}
                    onCheckedChange={toggleAll}
                    aria-label="Select all"
                  />
                </TableHead>
                <TableHead className="min-w-[180px]">
                  <SortableHeader label="Campaign" sortKey="title" sort={sort} onSort={handleSort} />
                </TableHead>
                <TableHead className="min-w-[120px] text-xs font-medium text-muted-foreground whitespace-nowrap">
                  Type
                </TableHead>
                <TableHead className="min-w-[90px]">
                  <SortableHeader label="Discount" sortKey="amountValue" sort={sort} onSort={handleSort} />
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
                <TableHead className="min-w-[80px] text-xs font-medium text-muted-foreground">
                  SKUs
                </TableHead>
                <TableHead className="w-[96px] text-xs font-medium text-muted-foreground whitespace-nowrap">
                  Conflicts
                </TableHead>
                <TableHead className="min-w-[100px] text-xs font-medium text-muted-foreground">
                  Status
                </TableHead>
                <TableHead className="pr-4 min-w-[52px]" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((d) => (
                <TableRow key={d.id} data-selected={selectedIds.has(d.id)}>
                  {/* Row checkbox */}
                  <TableCell className="pl-4">
                    <Checkbox
                      checked={selectedIds.has(d.id)}
                      onCheckedChange={() => toggleRow(d.id)}
                      aria-label={`Select ${d.title}`}
                    />
                  </TableCell>

                  {/* Campaign — colored title pill + optional coupon chip */}
                  <TableCell>
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <span
                        className="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold text-white max-w-[180px] truncate"
                        style={{ backgroundColor: `#${d.colorHex}` }}
                      >
                        {d.title}
                      </span>
                      {d.couponCode && (
                        <span className="inline-flex items-center gap-1 rounded-full border bg-primary/5 px-1.5 py-0.5 text-[10px] font-mono uppercase text-primary">
                          <Ticket className="h-2.5 w-2.5" />
                          {d.couponCode}
                        </span>
                      )}
                    </div>
                  </TableCell>

                  {/* Type */}
                  <TableCell>
                    <span className="inline-block rounded-md bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground whitespace-nowrap">
                      {d.type.name}
                    </span>
                  </TableCell>

                  {/* Discount amount */}
                  <TableCell>
                    <span className="text-sm font-bold tabular-nums text-rose-600 dark:text-rose-400">
                      −{d.amountValue}{d.amountType === "FIXED" ? " TJS" : "%"}
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

                  {/* SKUs — opens side drawer + target-type icon */}
                  <TableCell>
                    <div className="flex items-center gap-1.5">
                      {(() => {
                        const primaryType = d.targets[0]?.targetType ?? "SKU";
                        const Icon = primaryType === "CATEGORY" ? Tag : primaryType === "SEGMENT" ? Users : List;
                        return <Icon className="h-3.5 w-3.5 text-muted-foreground shrink-0" />;
                      })()}
                      <button
                        className="text-sm text-muted-foreground hover:text-foreground underline-offset-2 hover:underline transition-colors tabular-nums"
                        onClick={(e) => {
                          e.stopPropagation();
                          setDrawerCampaignId(d.id);
                        }}
                      >
                        {d.targets.length} {d.targets[0]?.targetType === "CATEGORY" ? "cat." : d.targets[0]?.targetType === "SEGMENT" ? "seg." : `SKU${d.targets.length !== 1 ? "s" : ""}`}
                      </button>
                    </div>
                  </TableCell>

                  {/* Conflicts — ⚠ badge with tooltip listing affected SKUs */}
                  <TableCell>
                    {(() => {
                      const items = overlapMap.get(d.id);
                      if (!items || items.length === 0) {
                        return (
                          <span className="text-muted-foreground/40 text-xs select-none">—</span>
                        );
                      }
                      const visible = items.slice(0, 10);
                      const extra = items.length - visible.length;
                      return (
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <span className="inline-flex items-center gap-1 cursor-default rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-700 select-none">
                              <AlertTriangle className="h-3 w-3 shrink-0" />
                              {items.length}
                            </span>
                          </TooltipTrigger>
                          <TooltipContent side="top" className="max-w-[260px] space-y-1 text-xs">
                            <p className="font-semibold mb-1">
                              Conflicts on {items.length} SKU{items.length !== 1 ? "s" : ""}
                            </p>
                            {visible.map((item, i) => (
                              <p key={i} className="text-muted-foreground truncate">
                                {item.skuCode} → winner: {item.winnerTitle}
                              </p>
                            ))}
                            {extra > 0 && (
                              <p className="text-muted-foreground/70">…and {extra} more</p>
                            )}
                          </TooltipContent>
                        </Tooltip>
                      );
                    })()}
                  </TableCell>

                  {/* Status */}
                  <TableCell>
                    <StatusBadge discount={d} />
                  </TableCell>

                  {/* Row actions */}
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

        {!isLoading && rows.length === 0 && (
          <div className="p-12 text-center text-muted-foreground">
            {debouncedSearch
              ? `No campaigns matching "${debouncedSearch}".`
              : "No discounts found."}
          </div>
        )}
      </div>


      {/* SKU detail drawer — controlled by row chip clicks */}
      <CampaignSkuDrawer
        campaignId={drawerCampaignId}
        onOpenChange={(open) => !open && setDrawerCampaignId(null)}
      />
    </div>
    </TooltipProvider>
  );
}
