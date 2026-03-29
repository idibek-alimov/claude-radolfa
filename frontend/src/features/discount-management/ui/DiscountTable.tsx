"use client";

import { useState } from "react";
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
import { Skeleton } from "@/shared/ui/skeleton";
import { fetchDiscounts, fetchDiscountTypes, enableDiscount, disableDiscount } from "../api";
import { DiscountStatusBadge } from "./DiscountStatusBadge";
import type { DiscountListFilters, DiscountResponse } from "../model/types";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";

const PAGE_SIZE = 15;

interface DiscountTableProps {
  onEdit: (discount: DiscountResponse) => void;
  onNew: () => void;
}

export function DiscountTable({ onEdit, onNew }: DiscountTableProps) {
  const [filters, setFilters] = useState<DiscountListFilters>({
    page: 1,
    size: PAGE_SIZE,
    status: "all",
  });

  const qc = useQueryClient();

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

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex flex-wrap items-end gap-3">
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Type</label>
          <select
            className="flex h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
            value={filters.typeId ?? ""}
            onChange={(e) =>
              setFilters((f) => ({
                ...f,
                page: 1,
                typeId: e.target.value ? Number(e.target.value) : undefined,
              }))
            }
          >
            <option value="">All types</option>
            {types.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Status</label>
          <select
            className="flex h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
            value={filters.status ?? "all"}
            onChange={(e) =>
              setFilters((f) => ({
                ...f,
                page: 1,
                status: e.target.value as DiscountListFilters["status"],
              }))
            }
          >
            <option value="all">All</option>
            <option value="active">Active</option>
            <option value="disabled">Disabled</option>
          </select>
        </div>

        <Button size="sm" className="ml-auto gap-1.5" onClick={onNew}>
          <Plus className="h-3.5 w-3.5" />
          New Discount
        </Button>
      </div>

      {/* Table */}
      <div className={`bg-card rounded-xl border shadow-sm transition-opacity ${isFetching && !isLoading ? "opacity-60" : ""}`}>
        {isLoading ? (
          <div className="p-6 space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">Campaign</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>SKUs</TableHead>
                <TableHead>Valid</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right pr-4">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data?.content.map((d) => (
                <TableRow key={d.id}>
                  <TableCell className="pl-4">
                    <DiscountStatusBadge discount={d} />
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {d.type.name}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {d.itemCodes.length} SKU{d.itemCodes.length !== 1 ? "s" : ""}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(d.validFrom)} – {formatDate(d.validUpto)}
                  </TableCell>
                  <TableCell>
                    <span
                      className={`text-xs font-medium ${
                        d.disabled ? "text-muted-foreground" : "text-green-600"
                      }`}
                    >
                      {d.disabled ? "Disabled" : "Active"}
                    </span>
                  </TableCell>
                  <TableCell className="text-right pr-4">
                    <div className="flex justify-end gap-1">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => toggleMutation.mutate({ id: d.id, disabled: d.disabled })}
                        disabled={toggleMutation.isPending}
                      >
                        {d.disabled ? "Enable" : "Disable"}
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => onEdit(d)}>
                        Edit
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {!isLoading && data?.content.length === 0 && (
          <div className="p-12 text-center text-muted-foreground">No discounts found.</div>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            {data.totalElements} discount{data.totalElements !== 1 ? "s" : ""} total
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
