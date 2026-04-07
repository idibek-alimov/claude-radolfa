"use client";

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuth } from "@/features/auth";
import { createCategory, deleteCategory } from "@/entities/category";
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
import { Folder, FolderPlus, Plus, Trash2, Loader2, AlertCircle, BookOpen } from "lucide-react";
import { useTranslations } from "next-intl";

export function CategoryManagementPanel() {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const { data: categories, isLoading } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
  });

  const flat = categories ? flattenTree(categories) : [];

  const [newName, setNewName] = useState("");
  const [newParentId, setNewParentId] = useState<number | "">("");
  const [formError, setFormError] = useState("");
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [expandedCategoryId, setExpandedCategoryId] = useState<number | null>(null);

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

  const renderTree = (nodes: CategoryTree[], depth = 0): React.ReactNode =>
    nodes.map((node) => (
      <div key={node.id}>
        <div
          className="flex items-center gap-2 py-1.5 rounded-md hover:bg-muted/50 group pr-2"
          style={{ paddingLeft: `${12 + depth * 20}px` }}
        >
          <Folder className="h-4 w-4 text-muted-foreground shrink-0" />
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
        {node.children.length > 0 && renderTree(node.children, depth + 1)}
      </div>
    ));

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
