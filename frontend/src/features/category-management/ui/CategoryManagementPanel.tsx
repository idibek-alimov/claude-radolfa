"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuth } from "@/features/auth";
import { createCategory, updateCategory, deleteCategory } from "@/entities/category";
import { fetchCategoryTree } from "@/entities/product/api";
import type { CategoryTree } from "@/entities/product/model/types";
import { BlueprintManagementPanel } from "@/features/blueprint-management";
import { flattenTree } from "../lib/flattenTree";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import { getErrorMessage } from "@/shared/lib";
import {
  Folder,
  FolderPlus,
  Plus,
  Trash2,
  Loader2,
  AlertCircle,
  BookOpen,
  Pencil,
  ChevronRight,
  ChevronDown,
} from "lucide-react";
import { useTranslations } from "next-intl";

const COLLAPSED_KEY = "radolfa.admin.collapsedCategoryIds";

function loadCollapsed(): Set<number> {
  try {
    const raw = localStorage.getItem(COLLAPSED_KEY);
    return raw ? new Set<number>(JSON.parse(raw)) : new Set<number>();
  } catch {
    return new Set<number>();
  }
}

function saveCollapsed(ids: Set<number>) {
  localStorage.setItem(COLLAPSED_KEY, JSON.stringify([...ids]));
}

function getDescendantIds(node: CategoryTree): number[] {
  return node.children.flatMap((child) => [child.id, ...getDescendantIds(child)]);
}

export function CategoryManagementPanel() {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const isManagerOrAdmin = user?.role === "MANAGER" || user?.role === "ADMIN";

  const { data: categories, isLoading } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
  });

  const flat = categories ? flattenTree(categories) : [];

  // --- Create form ---
  const [newName, setNewName] = useState("");
  const [newParentId, setNewParentId] = useState<number | "">("");
  const [formError, setFormError] = useState("");

  // --- Edit state ---
  const [editingCategory, setEditingCategory] = useState<CategoryTree | null>(null);
  const [editName, setEditName] = useState("");
  const [editParentId, setEditParentId] = useState<number | "">("");
  const [editError, setEditError] = useState("");

  // --- Delete state ---
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  // --- Blueprint panel ---
  const [expandedCategoryId, setExpandedCategoryId] = useState<number | null>(null);

  // --- Collapsible tree state (localStorage-backed) ---
  const [collapsedIds, setCollapsedIds] = useState<Set<number>>(new Set());

  // Hydrate collapsed state from localStorage on mount
  useEffect(() => {
    setCollapsedIds(loadCollapsed());
  }, []);

  // Initialise newly-loaded child nodes as collapsed by default
  useEffect(() => {
    if (!categories) return;
    setCollapsedIds((prev) => {
      const next = new Set(prev);
      let changed = false;
      const initDepth = (nodes: CategoryTree[], depth: number) => {
        nodes.forEach((node) => {
          if (depth > 0 && node.children.length > 0 && !prev.has(node.id)) {
            // Only auto-collapse if the user has never interacted with this node
            // (i.e., it's not explicitly in the saved set — but also not explicitly expanded)
            // We track collapsed ids; nodes absent from the set are expanded.
            // On first load, we want children collapsed, so we add them.
            next.add(node.id);
            changed = true;
          }
          initDepth(node.children, depth + 1);
        });
      };
      initDepth(categories, 0);
      if (changed) saveCollapsed(next);
      return changed ? next : prev;
    });
  }, [categories]); // eslint-disable-line react-hooks/exhaustive-deps

  const toggleCollapse = useCallback((id: number) => {
    setCollapsedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      saveCollapsed(next);
      return next;
    });
  }, []);

  // --- Mutations ---
  const createMutation = useMutation({
    mutationFn: () =>
      createCategory(newName.trim(), newParentId === "" ? null : Number(newParentId)),
    onSuccess: () => {
      toast.success(t("categoryCreated"));
      setNewName("");
      setNewParentId("");
      setFormError("");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (err: unknown) => setFormError(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: () =>
      updateCategory(editingCategory!.id, {
        name: editName.trim(),
        parentId: editParentId === "" ? null : Number(editParentId),
      }),
    onSuccess: () => {
      toast.success(t("categoryUpdated"));
      setEditingCategory(null);
      setEditError("");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (err: unknown) => setEditError(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteCategory(id),
    onSuccess: () => {
      toast.success(t("deletedCategory"));
      setConfirmDeleteId(null);
      queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (err: unknown) => {
      const axiosErr = err as { response?: { status?: number } };
      if (axiosErr?.response?.status === 422) {
        toast.error(t("categoryInUse"));
      } else {
        toast.error(getErrorMessage(err));
      }
      setConfirmDeleteId(null);
    },
  });

  const handleCreate = () => {
    if (!newName.trim()) { setFormError(t("fieldRequired")); return; }
    setFormError("");
    createMutation.mutate();
  };

  const openEdit = (node: CategoryTree) => {
    setEditingCategory(node);
    setEditName(node.name);
    setEditParentId(node.parentId ?? "");
    setEditError("");
  };

  const handleUpdate = () => {
    if (!editName.trim()) { setEditError(t("fieldRequired")); return; }
    setEditError("");
    updateMutation.mutate();
  };

  // Parent options for edit — excludes the node itself and its descendants
  const editParentOptions = editingCategory
    ? flat.filter(
        (cat) =>
          cat.id !== editingCategory.id &&
          !getDescendantIds(editingCategory).includes(cat.id)
      )
    : flat;

  const renderTree = (nodes: CategoryTree[], depth = 0): React.ReactNode =>
    nodes.map((node) => {
      const isCollapsed = collapsedIds.has(node.id);
      const hasChildren = node.children.length > 0;

      return (
        <div key={node.id}>
          <div
            className="flex items-center gap-2 py-1.5 rounded-md hover:bg-muted/50 group pr-2"
            style={{ paddingLeft: `${12 + depth * 20}px` }}
          >
            {hasChildren ? (
              <button
                onClick={() => toggleCollapse(node.id)}
                className="shrink-0 text-muted-foreground hover:text-foreground"
              >
                {isCollapsed ? (
                  <ChevronRight className="h-4 w-4" />
                ) : (
                  <ChevronDown className="h-4 w-4" />
                )}
              </button>
            ) : (
              <Folder className="h-4 w-4 text-muted-foreground shrink-0" />
            )}
            <span className="flex-1 text-sm">{node.name}</span>
            <code className="text-xs text-muted-foreground bg-muted px-1.5 py-0.5 rounded">
              {node.slug}
            </code>
            {isAdmin && (
              <Button
                variant="ghost"
                size="sm"
                className="h-6 px-2 opacity-0 group-hover:opacity-100 gap-1 text-muted-foreground"
                onClick={() =>
                  setExpandedCategoryId((prev) => prev === node.id ? null : node.id)
                }
              >
                <BookOpen className="h-3.5 w-3.5" />
                <span className="text-xs">Blueprint</span>
              </Button>
            )}
            {isManagerOrAdmin && (
              <Button
                variant="ghost"
                size="icon"
                className="h-6 w-6 opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-foreground"
                onClick={() => openEdit(node)}
              >
                <Pencil className="h-3.5 w-3.5" />
              </Button>
            )}
            {isAdmin && (
              <Button
                variant="ghost"
                size="icon"
                className="h-6 w-6 opacity-0 group-hover:opacity-100 text-destructive hover:text-destructive hover:bg-destructive/10"
                onClick={() => setConfirmDeleteId(node.id)}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </Button>
            )}
          </div>
          {isAdmin && expandedCategoryId === node.id && (
            <div
              className="border-l ml-6 pl-4 pb-2"
              style={{ marginLeft: `${12 + depth * 20 + 20}px` }}
            >
              <BlueprintManagementPanel categoryId={node.id} categoryName={node.name} />
            </div>
          )}
          {hasChildren && !isCollapsed && renderTree(node.children, depth + 1)}
        </div>
      );
    });

  if (isLoading) {
    return (
      <div className="bg-card rounded-xl border shadow-sm p-6 space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-8 w-full" />
        ))}
      </div>
    );
  }

  return (
    <>
      <div className="space-y-4">
        <div className="bg-card rounded-xl border shadow-sm p-4">
          <h3 className="text-sm font-semibold mb-3 flex items-center gap-1.5">
            <FolderPlus className="h-4 w-4" />
            {t("newCategoryTitle")}
          </h3>
          <div className="flex flex-col sm:flex-row gap-2">
            <Input
              value={newName}
              onChange={(e) => { setNewName(e.target.value); setFormError(""); }}
              placeholder={t("categoryNameLabel")}
              className="flex-1"
            />
            <select
              value={newParentId}
              onChange={(e) =>
                setNewParentId(e.target.value === "" ? "" : Number(e.target.value))
              }
              className="flex h-9 w-full sm:w-52 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
            >
              <option value="">{t("noParent")}</option>
              {flat.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {"\u00a0\u00a0".repeat(cat.depth)}{cat.name}
                </option>
              ))}
            </select>
            <Button onClick={handleCreate} disabled={createMutation.isPending} className="gap-1.5">
              {createMutation.isPending ? (
                <><Loader2 className="h-4 w-4 animate-spin" />{t("creatingCategory")}</>
              ) : (
                <><Plus className="h-4 w-4" />{t("createCategoryButton")}</>
              )}
            </Button>
          </div>
          {formError && (
            <p className="mt-2 text-sm text-destructive flex items-center gap-1.5">
              <AlertCircle className="h-3.5 w-3.5" />
              {formError}
            </p>
          )}
        </div>

        <div className="bg-card rounded-xl border shadow-sm">
          <div className="px-4 py-3 border-b">
            <h3 className="text-sm font-semibold">{t("categoryTitle")}</h3>
          </div>
          <div className="p-2">
            {categories && categories.length > 0 ? (
              renderTree(categories)
            ) : (
              <p className="text-sm text-muted-foreground text-center py-8">
                {t("noCategoriesFound")}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Edit category dialog */}
      <Dialog
        open={editingCategory !== null}
        onOpenChange={(open) => !open && setEditingCategory(null)}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{t("editCategoryTitle")}</DialogTitle>
            <DialogDescription>{t("editCategoryDesc")}</DialogDescription>
          </DialogHeader>
          <div className="space-y-3 py-2">
            <Input
              value={editName}
              onChange={(e) => { setEditName(e.target.value); setEditError(""); }}
              placeholder={t("categoryNameLabel")}
            />
            <select
              value={editParentId}
              onChange={(e) =>
                setEditParentId(e.target.value === "" ? "" : Number(e.target.value))
              }
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
            >
              <option value="">{t("noParent")}</option>
              {editParentOptions.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {"\u00a0\u00a0".repeat(cat.depth)}{cat.name}
                </option>
              ))}
            </select>
            {editError && (
              <p className="text-sm text-destructive flex items-center gap-1.5">
                <AlertCircle className="h-3.5 w-3.5" />
                {editError}
              </p>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditingCategory(null)}>
              {t("cancel")}
            </Button>
            <Button onClick={handleUpdate} disabled={updateMutation.isPending}>
              {updateMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                t("save")
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete confirm dialog */}
      <Dialog
        open={confirmDeleteId !== null}
        onOpenChange={(open) => !open && setConfirmDeleteId(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t("confirmDeleteTitle")}</DialogTitle>
            <DialogDescription>{t("confirmDeleteDesc")}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmDeleteId(null)}>
              {t("cancel")}
            </Button>
            <Button
              variant="destructive"
              disabled={deleteMutation.isPending}
              onClick={() =>
                confirmDeleteId !== null && deleteMutation.mutate(confirmDeleteId)
              }
            >
              {deleteMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                t("confirmDeleteButton")
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
