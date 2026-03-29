"use client";

import React, { useState, useCallback, useRef } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { useAuth } from "@/features/auth";
import { UserManagementTable } from "@/features/user-management";
import { toast } from "sonner";
import {
  useQuery,
  useMutation,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import {
  fetchListings,
  searchListings,
} from "@/entities/product/api";
import { fetchCategoryTree } from "@/entities/product/api";
import type { ListingVariant, CategoryTree, Color } from "@/entities/product/model/types";
import { useLoyaltyTiers, updateTierColor } from "@/entities/loyalty";
import type { LoyaltyTier } from "@/entities/loyalty";
import { createCategory, deleteCategory } from "@/entities/category";
import { fetchColors, updateColor } from "@/entities/color";
import { reindexSearch } from "@/features/search/api";
import { ReviewModerationQueue } from "@/features/review-moderation";
import { QuestionModerationQueue } from "@/features/question-moderation";
import type { ReindexResult } from "@/features/search/api";
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/shared/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/shared/ui/dialog";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/tabs";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Badge } from "@/shared/ui/badge";
import { Skeleton } from "@/shared/ui/skeleton";
import { getErrorMessage } from "@/shared/lib";
import { Pencil, Lock, Search, Package, Users, ChevronLeft, ChevronRight, Award, Plus, Folder, FolderPlus, Palette, RefreshCw, Trash2, Loader2, AlertCircle, Check } from "lucide-react";
import { useTranslations } from "next-intl";

// ── Helpers ─────────────────────────────────────────────────────────
interface FlatCategory { id: number; name: string; depth: number; }
function flattenTree(nodes: CategoryTree[], depth = 0): FlatCategory[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, depth },
    ...flattenTree(node.children, depth + 1),
  ]);
}

// ── Page ─────────────────────────────────────────────────────────────
export default function ManagePage() {
  const t = useTranslations("manage");
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const queryClient = useQueryClient();

  const [reindexResult, setReindexResult] = useState<ReindexResult | null>(null);
  const reindexMutation = useMutation({
    mutationFn: reindexSearch,
    onSuccess: (result) => setReindexResult(result),
    onError: (err: unknown) => toast.error(getErrorMessage(err, "Reindex failed")),
  });

  return (
    <ProtectedRoute requiredRole="MANAGER">
      <div className="min-h-screen bg-muted/30 py-10">
        <div className="max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="mb-8">
            <h1 className="text-2xl sm:text-3xl font-bold text-foreground">
              {t("dashboardTitle")}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {t("dashboardSubtitle")}
            </p>
          </div>

          <Tabs defaultValue="products" className="space-y-6">
            <TabsList>
              <TabsTrigger value="products" className="gap-1.5">
                <Package className="h-4 w-4" />
                {t("tabProducts")}
              </TabsTrigger>
              <TabsTrigger value="users" className="gap-1.5">
                <Users className="h-4 w-4" />
                {t("tabUsers")}
              </TabsTrigger>
              <TabsTrigger value="loyalty-tiers" className="gap-1.5">
                <Award className="h-4 w-4" />
                {t("tabLoyaltyTiers")}
              </TabsTrigger>
              <TabsTrigger value="categories" className="gap-1.5">
                <Folder className="h-4 w-4" />
                {t("tabCategories")}
              </TabsTrigger>
              <TabsTrigger value="colors" className="gap-1.5">
                <Palette className="h-4 w-4" />
                {t("tabColors")}
              </TabsTrigger>
              <TabsTrigger value="reviews">Reviews</TabsTrigger>
              {isAdmin && <TabsTrigger value="qa">Q&A</TabsTrigger>}
            </TabsList>

            <TabsContent value="products">
              <ProductManagement />
            </TabsContent>

            <TabsContent value="users">
              <UserManagementTable />
            </TabsContent>

            <TabsContent value="loyalty-tiers">
              <LoyaltyTierManagement />
            </TabsContent>

            <TabsContent value="categories">
              <CategoryManagement />
            </TabsContent>

            <TabsContent value="colors">
              <ColorManagement />
            </TabsContent>

            <TabsContent value="reviews">
              <ReviewModerationQueue />
            </TabsContent>
            {isAdmin && (
              <TabsContent value="qa">
                <QuestionModerationQueue />
              </TabsContent>
            )}
          </Tabs>

          {/* Search Tools (ADMIN only) */}
          {isAdmin && (
            <div className="bg-card rounded-xl border shadow-sm p-4">
              <h3 className="text-sm font-semibold mb-3 flex items-center gap-1.5">
                <RefreshCw className="h-4 w-4" />
                {t("searchToolsTitle")}
              </h3>
              <div className="flex items-center gap-3 flex-wrap">
                <Button
                  variant="outline"
                  onClick={() => { setReindexResult(null); reindexMutation.mutate(); }}
                  disabled={reindexMutation.isPending}
                  className="gap-1.5"
                >
                  {reindexMutation.isPending ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      {t("reindexing")}
                    </>
                  ) : (
                    <>
                      <RefreshCw className="h-4 w-4" />
                      {t("reindexButton")}
                    </>
                  )}
                </Button>
                {reindexResult && (
                  <p className="text-sm text-muted-foreground">
                    {t("reindexResult", { count: reindexResult.indexed, errors: reindexResult.errorCount })}
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </ProtectedRoute>
  );
}

// ── Product Management ───────────────────────────────────────────

function ProductManagement() {
  const t = useTranslations("manage");
  const router = useRouter();

  // ── Query State ─────────────────────────────────────────────────
  const [page, setPage] = useState(1);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const debounceRef = useRef<NodeJS.Timeout | undefined>(undefined);

  const handleSearchChange = useCallback((value: string) => {
    setSearchQuery(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim());
      setPage(1);
    }, 300);
  }, []);

  // ── Data Fetching ──────────────────────────────────────────────
  const { data, isLoading } = useQuery({
    queryKey: ["listings", page, debouncedSearch],
    queryFn: () =>
      debouncedSearch
        ? searchListings(debouncedSearch, page, 12)
        : fetchListings(page, 12),
    placeholderData: keepPreviousData,
  });

  const listings = data?.content ?? [];

  const displayPrice = (item: ListingVariant) => `${item.originalPrice.toFixed(2)} TJS`;
  const computeStock = (item: ListingVariant) =>
    item.skus.reduce((acc, s) => acc + s.stockQuantity, 0);

  return (
    <>
      {/* Toolbar: Search + New Product */}
      <div className="mb-4 flex items-center gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder={t("searchByName")}
            className="pl-9"
          />
        </div>
        <Button className="gap-1.5" onClick={() => router.push("/manage/products/create")}>
          <Plus className="h-4 w-4" />
          {t("newProduct")}
        </Button>
      </div>

      {/* Product Table */}
      <div className="bg-card rounded-xl border shadow-sm">
        {isLoading && listings.length === 0 ? (
          <div className="p-6 space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4 w-[56px]">{t("tableImage")}</TableHead>
                <TableHead>{t("tableProduct")}</TableHead>
                <TableHead>{t("tableSlugKey")}</TableHead>
                <TableHead>{t("tablePrice")}</TableHead>
                <TableHead>{t("tableStock")}</TableHead>
                <TableHead>{t("tableStatus")}</TableHead>
                <TableHead className="text-right pr-4">{t("tableActions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {listings.map((item) => (
                <TableRow key={item.variantId}>
                  <TableCell className="pl-4">
                    {item.images?.[0] ? (
                      <div className="relative h-10 w-10 rounded-md border overflow-hidden">
                        <Image
                          src={item.images[0]}
                          alt={item.colorDisplayName}
                          width={40}
                          height={40}
                          className="object-cover aspect-square"
                          unoptimized
                        />
                      </div>
                    ) : (
                      <div className="flex h-10 w-10 items-center justify-center rounded-md bg-muted">
                        <Package className="h-4 w-4 text-muted-foreground" />
                      </div>
                    )}
                  </TableCell>
                  <TableCell>
                    <div>
                      <p className="font-medium text-sm">{item.colorDisplayName}</p>
                      <p className="text-xs text-muted-foreground truncate max-w-xs">
                        {item.productCode}
                      </p>
                    </div>
                  </TableCell>
                  <TableCell>
                    <code className="text-xs bg-muted px-1.5 py-0.5 rounded">
                      {item.slug}
                    </code>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <Lock className="h-3 w-3 text-muted-foreground" />
                      <span className="text-sm">{displayPrice(item)}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <Lock className="h-3 w-3 text-muted-foreground" />
                      <span className="text-sm">{computeStock(item)}</span>
                    </div>
                  </TableCell>
                  <TableCell />
                  <TableCell className="text-right pr-4">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => router.push(`/manage/products/${item.slug}/edit`)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {listings.length === 0 && !isLoading && (
          <div className="p-12 text-center text-muted-foreground">
            {debouncedSearch
              ? t("noProductsMatching", { search: debouncedSearch })
              : t("noProductsFound")}
          </div>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-muted-foreground">
            {t("productsTotal", { count: data.totalElements })}
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page <= 1}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="text-sm text-muted-foreground">{t("page", { page })}</span>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
              onClick={() => setPage((p) => p + 1)}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}

    </>
  );
}

// ── Loyalty Tier Management ──────────────────────────────────────

function LoyaltyTierManagement() {
  const t = useTranslations("manage");
  const pt = useTranslations("profile");
  const queryClient = useQueryClient();
  const { data: tiers, isLoading } = useLoyaltyTiers();

  // Track pending color changes per tier id
  const [pendingColors, setPendingColors] = useState<Record<number, string>>({});

  const colorMutation = useMutation({
    mutationFn: ({ id, color }: { id: number; color: string }) =>
      updateTierColor(id, color),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["loyalty-tiers"] });
      setPendingColors((prev) => {
        const next = { ...prev };
        delete next[variables.id];
        return next;
      });
      toast.success(t("colorUpdated"));
    },
    onError: (err: unknown) => {
      toast.error(getErrorMessage(err, "Failed to update color"));
    },
  });

  const handleColorChange = (tier: LoyaltyTier, color: string) => {
    setPendingColors((prev) => ({
      ...prev,
      [tier.id]: color,
    }));
  };

  const handleSaveColor = (tier: LoyaltyTier) => {
    const color = pendingColors[tier.id];
    if (!color) return;
    colorMutation.mutate({ id: tier.id, color });
  };

  const hasChanged = (tier: LoyaltyTier) =>
    pendingColors[tier.id] !== undefined && pendingColors[tier.id] !== tier.color;

  if (isLoading) {
    return (
      <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-14 w-full" />
        ))}
      </div>
    );
  }

  if (!tiers || tiers.length === 0) {
    return (
      <div className="bg-card rounded-xl border shadow-sm p-12 text-center text-muted-foreground">
        {t("loyaltyTiers")}
      </div>
    );
  }

  return (
    <div className="bg-card rounded-xl border shadow-sm">
      <div className="px-6 py-4 border-b">
        <h2 className="text-lg font-semibold">{t("loyaltyTiers")}</h2>
      </div>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="pl-4 w-[48px]">{t("tierColor")}</TableHead>
            <TableHead>{pt("allTiers")}</TableHead>
            <TableHead>{pt("discount")}</TableHead>
            <TableHead>{pt("cashback")}</TableHead>
            <TableHead>{pt("minSpend")}</TableHead>
            <TableHead>{t("tierColor")}</TableHead>
            <TableHead className="text-right pr-4">{t("tableActions")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {tiers.map((tier) => (
            <TableRow key={tier.id}>
              {/* Color swatch */}
              <TableCell className="pl-4">
                <div
                  className="h-6 w-6 rounded-full border"
                  style={{ backgroundColor: pendingColors[tier.id] ?? tier.color }}
                />
              </TableCell>

              {/* Name (read-only, system-managed) */}
              <TableCell>
                <div className="flex items-center gap-1.5">
                  <Lock className="h-3 w-3 text-muted-foreground shrink-0" />
                  <span className="font-medium text-sm">{tier.name}</span>
                </div>
              </TableCell>

              {/* Discount % (read-only, system-managed) */}
              <TableCell>
                <div className="flex items-center gap-1">
                  <Lock className="h-3 w-3 text-muted-foreground" />
                  <span className="text-sm">{tier.discountPercentage}%</span>
                </div>
              </TableCell>

              {/* Cashback % (read-only, system-managed) */}
              <TableCell>
                <div className="flex items-center gap-1">
                  <Lock className="h-3 w-3 text-muted-foreground" />
                  <span className="text-sm">{tier.cashbackPercentage}%</span>
                </div>
              </TableCell>

              {/* Min spend (read-only, system-managed) */}
              <TableCell>
                <div className="flex items-center gap-1">
                  <Lock className="h-3 w-3 text-muted-foreground" />
                  <span className="text-sm">{tier.minSpendRequirement} TJS</span>
                </div>
              </TableCell>

              {/* Color picker (editable) */}
              <TableCell>
                <input
                  type="color"
                  value={pendingColors[tier.id] ?? tier.color}
                  onChange={(e) => handleColorChange(tier, e.target.value)}
                  className="h-8 w-12 cursor-pointer rounded border border-input bg-transparent p-0.5"
                />
              </TableCell>

              {/* Save button */}
              <TableCell className="text-right pr-4">
                {hasChanged(tier) && (
                  <Button
                    size="sm"
                    onClick={() => handleSaveColor(tier)}
                    disabled={colorMutation.isPending}
                    className="gap-1"
                  >
                    {colorMutation.isPending ? (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    ) : (
                      <Check className="h-3.5 w-3.5" />
                    )}
                    {t("save")}
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

// ── Category Management ──────────────────────────────────────────────

function CategoryManagement() {
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
              size="icon"
              className="h-6 w-6 opacity-0 group-hover:opacity-100 text-destructive hover:text-destructive hover:bg-destructive/10"
              onClick={() => setConfirmDeleteId(node.id)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
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
        {/* New Category Form */}
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
            <Button
              onClick={handleCreate}
              disabled={createMutation.isPending}
              className="gap-1.5"
            >
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

        {/* Category Tree */}
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

      {/* Delete Confirm Dialog */}
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

// ── Color Management ─────────────────────────────────────────────────

function ColorManagement() {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();

  const { data: colors, isLoading } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  const [pending, setPending] = useState<
    Record<number, { displayName: string; hexCode: string }>
  >({});

  const updateMutation = useMutation({
    mutationFn: ({
      id,
      displayName,
      hexCode,
    }: {
      id: number;
      displayName: string;
      hexCode: string;
    }) => updateColor(id, displayName, hexCode),
    onSuccess: (result) => {
      toast.success(t("colorSaved"));
      setPending((prev) => {
        const next = { ...prev };
        delete next[result.id];
        return next;
      });
      queryClient.invalidateQueries({ queryKey: ["colors"] });
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const getDisplayName = (color: Color) =>
    pending[color.id]?.displayName ?? color.displayName ?? "";
  const getHexCode = (color: Color) =>
    pending[color.id]?.hexCode ?? color.hexCode ?? "#000000";

  const handleChange = (
    color: Color,
    field: "displayName" | "hexCode",
    value: string
  ) => {
    setPending((prev) => ({
      ...prev,
      [color.id]: {
        displayName: getDisplayName(color),
        hexCode: getHexCode(color),
        [field]: value,
      },
    }));
  };

  const hasChanged = (color: Color) => {
    const p = pending[color.id];
    if (!p) return false;
    return (
      p.displayName !== (color.displayName ?? "") ||
      p.hexCode !== (color.hexCode ?? "#000000")
    );
  };

  if (isLoading) {
    return (
      <div className="bg-card rounded-xl border shadow-sm p-6 space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    );
  }

  if (!colors || colors.length === 0) {
    return (
      <div className="bg-card rounded-xl border shadow-sm p-12 text-center text-muted-foreground">
        {t("noColorsFound")}
      </div>
    );
  }

  return (
    <div className="bg-card rounded-xl border shadow-sm">
      <div className="px-6 py-4 border-b">
        <h2 className="text-lg font-semibold">{t("colorTitle")}</h2>
      </div>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="pl-4 w-[48px]">{t("tierColor")}</TableHead>
            <TableHead>{t("colorKey")}</TableHead>
            <TableHead>{t("colorDisplayNameLabel")}</TableHead>
            <TableHead>{t("colorHexLabel")}</TableHead>
            <TableHead className="text-right pr-4">{t("tableActions")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {colors.map((color) => {
            const displayName = getDisplayName(color);
            const hexCode = getHexCode(color);
            return (
              <TableRow key={color.id}>
                <TableCell className="pl-4">
                  <div
                    className="h-6 w-6 rounded-full border"
                    style={{ backgroundColor: hexCode }}
                  />
                </TableCell>
                <TableCell>
                  <code className="text-xs bg-muted px-1.5 py-0.5 rounded">
                    {color.colorKey}
                  </code>
                </TableCell>
                <TableCell>
                  <Input
                    value={displayName}
                    onChange={(e) => handleChange(color, "displayName", e.target.value)}
                    className="h-8 text-sm max-w-[180px]"
                  />
                </TableCell>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <input
                      type="color"
                      value={hexCode}
                      onChange={(e) => handleChange(color, "hexCode", e.target.value)}
                      className="h-8 w-10 cursor-pointer rounded border border-input bg-transparent p-0.5"
                    />
                    <Input
                      value={hexCode}
                      onChange={(e) => handleChange(color, "hexCode", e.target.value)}
                      className="h-8 text-sm w-24 font-mono"
                    />
                  </div>
                </TableCell>
                <TableCell className="text-right pr-4">
                  {hasChanged(color) && (
                    <Button
                      size="sm"
                      onClick={() =>
                        updateMutation.mutate({
                          id: color.id,
                          displayName,
                          hexCode,
                        })
                      }
                      disabled={updateMutation.isPending}
                      className="gap-1"
                    >
                      {updateMutation.isPending ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : (
                        <Check className="h-3.5 w-3.5" />
                      )}
                      {t("save")}
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
