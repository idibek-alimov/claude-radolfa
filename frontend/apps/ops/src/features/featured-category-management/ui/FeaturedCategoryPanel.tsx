"use client";

import { useCallback, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient, keepPreviousData } from "@tanstack/react-query";
import { toast } from "sonner";
import Image from "next/image";
import { ChevronLeft, ChevronRight, ImageOff, Pencil, Plus, Search, Trash2 } from "lucide-react";

import { fetchCategoryTree } from "@/entities/product/api";
import {
  fetchFeaturedCategories,
  deleteFeaturedCategory,
  updateFeaturedCategory,
  type FeaturedCategory,
} from "@/entities/featured-category";

import { Button } from "@radolfa/shared/ui/button";
import { Input } from "@radolfa/shared/ui/input";
import { Badge } from "@radolfa/shared/ui/badge";
import { Switch } from "@radolfa/shared/ui/switch";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@radolfa/shared/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@radolfa/shared/ui/table";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@radolfa/shared/ui/alert-dialog";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@radolfa/shared/ui/tooltip";
import { getErrorMessage } from "@radolfa/shared/lib";

import { flattenTree } from "../lib/flattenTree";
import { FeaturedCategoryForm } from "./FeaturedCategoryForm";

const PAGE_SIZE = 20;

const SORT_OPTIONS = [
  { value: "displayOrder,asc", label: "Order (low → high)" },
  { value: "displayOrder,desc", label: "Order (high → low)" },
  { value: "createdAt,desc", label: "Newest first" },
] as const;

export function FeaturedCategoryPanel() {
  const qc = useQueryClient();

  const [page, setPage] = useState(1);
  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [sort, setSort] = useState<string>(SORT_OPTIONS[0].value);
  const [formOpen, setFormOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<FeaturedCategory | null>(null);
  const [deletingEntry, setDeletingEntry] = useState<FeaturedCategory | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  const handleSearchChange = useCallback((value: string) => {
    setSearchInput(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim());
      setPage(1);
    }, 300);
  }, []);

  function handleSortChange(value: string) {
    setSort(value);
    setPage(1);
  }

  // Server-driven page/search/sort — never filter or sort `content[]` locally.
  const { data, isLoading } = useQuery({
    queryKey: ["featured-categories", page, debouncedSearch, sort],
    queryFn: () => fetchFeaturedCategories({ page, size: PAGE_SIZE, search: debouncedSearch, sort }),
    placeholderData: keepPreviousData,
  });

  // Category tree only powers the <Select> options and the id→name display
  // column — the admin DTO doesn't carry the category name.
  const { data: categoryTree } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
  });
  const flatCategories = categoryTree ? flattenTree(categoryTree) : [];
  const categoryNameById = new Map(flatCategories.map((c) => [c.id, c.name] as const));

  const entries = data?.content ?? [];

  const toggleActiveMutation = useMutation({
    mutationFn: (target: FeaturedCategory) =>
      updateFeaturedCategory(target.id, {
        categoryId: target.categoryId,
        imageUrl: target.imageUrl,
        title: target.title,
        subtitle: target.subtitle,
        displayOrder: target.displayOrder,
        active: !target.active,
      }),
    onSuccess: (updated) => {
      qc.invalidateQueries({ queryKey: ["featured-categories"] });
      toast.success(updated.active ? "Entry activated" : "Entry deactivated");
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteFeaturedCategory(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["featured-categories"] });
      setDeletingEntry(null);
      toast.success("Featured category removed");
    },
    onError: (err: unknown) => {
      setDeletingEntry(null);
      toast.error(getErrorMessage(err));
    },
  });

  function openCreate() {
    setEditingEntry(null);
    setFormOpen(true);
  }

  function openEdit(entry: FeaturedCategory) {
    setEditingEntry(entry);
    setFormOpen(true);
  }

  function nameFor(entry: FeaturedCategory | null): string {
    if (!entry) return "this entry";
    return entry.title || categoryNameById.get(entry.categoryId) || `Category #${entry.categoryId}`;
  }

  return (
    <TooltipProvider>
      <div className="flex flex-col flex-1 min-h-0 gap-4">
        {/* Toolbar */}
        <div className="flex items-center gap-3">
          <div className="relative max-w-sm flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              value={searchInput}
              onChange={(e) => handleSearchChange(e.target.value)}
              placeholder="Search by title or subtitle"
              className="pl-9"
            />
          </div>
          <Select value={sort} onValueChange={handleSortChange}>
            <SelectTrigger className="w-52">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {SORT_OPTIONS.map((opt) => (
                <SelectItem key={opt.value} value={opt.value}>
                  {opt.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button size="sm" className="gap-1.5" onClick={openCreate}>
            <Plus className="h-4 w-4" />
            New Entry
          </Button>
        </div>

        {/* Table */}
        <div className="flex-1 min-h-0 overflow-auto bg-card rounded-xl border shadow-sm">
          {isLoading && entries.length === 0 ? (
            <div className="p-6 space-y-4">
              {Array.from({ length: 5 }).map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : entries.length === 0 ? (
            <div className="flex flex-col items-center justify-center rounded-xl border border-dashed p-12 m-6 text-center">
              <ImageOff className="h-10 w-10 text-muted-foreground/40 mb-3" />
              <p className="text-sm text-muted-foreground">
                {debouncedSearch
                  ? `No entries matching "${debouncedSearch}".`
                  : "No featured categories yet — the homepage falls back to auto-selected categories."}
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-4">Image</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Title</TableHead>
                  <TableHead>Order</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right pr-4">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {entries.map((entry) => (
                  <TableRow key={entry.id} className={!entry.active ? "opacity-60" : ""}>
                    <TableCell className="pl-4">
                      <div className="relative h-12 w-12 rounded-lg overflow-hidden border bg-muted/40">
                        {entry.imageUrl ? (
                          <Image src={entry.imageUrl} alt="" fill unoptimized className="object-cover" />
                        ) : (
                          <div className="h-full w-full flex items-center justify-center">
                            <ImageOff className="h-4 w-4 text-muted-foreground/40" />
                          </div>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-sm font-medium">
                      {categoryNameById.get(entry.categoryId) ?? `#${entry.categoryId}`}
                    </TableCell>
                    <TableCell className="text-sm">
                      {entry.title || <span className="text-muted-foreground">—</span>}
                    </TableCell>
                    <TableCell className="text-sm tabular-nums">{entry.displayOrder}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Switch
                          checked={entry.active}
                          disabled={toggleActiveMutation.isPending}
                          onCheckedChange={() => toggleActiveMutation.mutate(entry)}
                        />
                        {entry.active ? (
                          <Badge variant="success">Active</Badge>
                        ) : (
                          <Badge variant="secondary">Inactive</Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-right pr-4">
                      <div className="flex items-center justify-end gap-1">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button variant="ghost" size="sm" onClick={() => openEdit(entry)}>
                              <Pencil className="h-3.5 w-3.5" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Edit entry</TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-rose-500 hover:text-rose-600 hover:bg-rose-50"
                              onClick={() => setDeletingEntry(entry)}
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Delete entry</TooltipContent>
                        </Tooltip>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>

        {/* Pagination */}
        {data && data.totalElements > 0 && (
          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <span>
              Showing {(page - 1) * PAGE_SIZE + 1}–
              {Math.min(page * PAGE_SIZE, data.totalElements)} of {data.totalElements}
            </span>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => setPage((p) => p - 1)} disabled={page <= 1}>
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button variant="outline" size="sm" onClick={() => setPage((p) => p + 1)} disabled={data.last}>
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </div>

      <FeaturedCategoryForm
        open={formOpen}
        onOpenChange={setFormOpen}
        entry={editingEntry}
        categories={flatCategories}
      />

      <AlertDialog
        open={!!deletingEntry}
        onOpenChange={(open) => {
          if (!open) setDeletingEntry(null);
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete &quot;{nameFor(deletingEntry)}&quot;?</AlertDialogTitle>
            <AlertDialogDescription>
              This featured category entry will be permanently removed from the homepage
              spotlight. This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-rose-600 hover:bg-rose-700 text-white"
              onClick={() => deletingEntry && deleteMutation.mutate(deletingEntry.id)}
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </TooltipProvider>
  );
}
