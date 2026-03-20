"use client";

import React, { useState, useCallback, useRef } from "react";
import Image from "next/image";
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
  fetchListingBySlug,
  updateListing,
  uploadListingImage,
  removeListingImage,
  searchListings,
} from "@/entities/product/api";
import { updateSkuPrice, updateSkuStock } from "@/entities/product/api/admin";
import { fetchCategoryTree } from "@/entities/product/api";
import type { ListingVariant, ListingVariantDetail, CategoryTree, Color } from "@/entities/product/model/types";
import { useLoyaltyTiers, updateTierColor } from "@/entities/loyalty";
import type { LoyaltyTier } from "@/entities/loyalty";
import { createCategory, deleteCategory } from "@/entities/category";
import { fetchColors, updateColor } from "@/entities/color";
import { reindexSearch } from "@/features/search/api";
import type { ReindexResult } from "@/features/search/api";
import { CreateProductDialog } from "@/features/product-creation";
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
import { Pencil, Lock, AlertCircle, Search, Package, Upload, Loader2, X, Star, Users, ChevronLeft, ChevronRight, Award, Check, Plus, Folder, FolderPlus, Palette, RefreshCw, Trash2 } from "lucide-react";
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

// ── Product Management (extracted from original page) ───────────────

function ProductManagement() {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  // ── Create dialog state ──────────────────────────────────────────
  const [isCreateOpen, setIsCreateOpen] = useState(false);

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

  // ── Dialog State ────────────────────────────────────────────────
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<ListingVariant | null>(null);
  const [saveError, setSaveError] = useState("");

  // Form State
  const [formData, setFormData] = useState({
    webDescription: "",
    topSelling: false,
    featured: false,
  });

  // Detail data (SKUs) loaded when dialog opens
  const [detail, setDetail] = useState<ListingVariantDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // Image upload state
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // ── Mutations ──────────────────────────────────────────────────
  const updateMutation = useMutation({
    mutationFn: ({ slug, data }: { slug: string; data: any }) =>
      updateListing(slug, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
      toast.success(t("productUpdated"));
      setIsDialogOpen(false);
    },
    onError: (err: unknown) => {
      setSaveError(getErrorMessage(err, "Failed to update"));
    },
  });

  const uploadMutation = useMutation({
    mutationFn: ({ slug, file }: { slug: string; file: File }) =>
      uploadListingImage(slug, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
      toast.success(t("imageUploaded"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err, t("uploadFailed"))),
  });

  const deleteImageMutation = useMutation({
    mutationFn: ({ slug, url }: { slug: string; url: string }) =>
      removeListingImage(slug, url),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
      toast.success(t("imageRemoved"));
      if (editingProduct) {
        setEditingProduct({
          ...editingProduct,
          images: editingProduct.images.filter(url => url !== variables.url)
        });
      }
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err, t("failedToRemoveImage"))),
  });

  // ── ADMIN: SKU Price / Stock editing ─────────────────────────
  const [pendingPrices, setPendingPrices] = useState<Record<number, string>>({});
  const [pendingStocks, setPendingStocks] = useState<Record<number, string>>({});

  const skuPriceMutation = useMutation({
    mutationFn: ({ skuId, price }: { skuId: number; price: number }) =>
      updateSkuPrice(skuId, price),
    onSuccess: (result) => {
      toast.success(t("priceUpdated"));
      if (detail) {
        setDetail({
          ...detail,
          skus: detail.skus.map((s) =>
            s.skuId === result.skuId ? { ...s, price: result.price } : s
          ),
        });
      }
      setPendingPrices((prev) => {
        const next = { ...prev };
        delete next[result.skuId];
        return next;
      });
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const skuStockMutation = useMutation({
    mutationFn: ({ skuId, quantity }: { skuId: number; quantity: number }) =>
      updateSkuStock(skuId, { quantity }),
    onSuccess: (result) => {
      toast.success(t("stockUpdated"));
      if (detail) {
        setDetail({
          ...detail,
          skus: detail.skus.map((s) =>
            s.skuId === result.skuId ? { ...s, stockQuantity: result.stockQuantity } : s
          ),
        });
      }
      setPendingStocks((prev) => {
        const next = { ...prev };
        delete next[result.skuId];
        return next;
      });
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const handleSkuPriceBlur = (skuId: number) => {
    const raw = pendingPrices[skuId];
    if (raw === undefined) return;
    const price = parseFloat(raw);
    if (isNaN(price) || price < 0) return;
    skuPriceMutation.mutate({ skuId, price });
  };

  const handleSkuStockSave = (skuId: number) => {
    const raw = pendingStocks[skuId];
    if (raw === undefined) return;
    const quantity = parseInt(raw, 10);
    if (isNaN(quantity) || quantity < 0) return;
    skuStockMutation.mutate({ skuId, quantity });
  };

  // ── Handlers ────────────────────────────────────────────────────

  const handleOpenDialog = async (product: ListingVariant) => {
    setSaveError("");
    setEditingProduct(product);
    setFormData({
      webDescription: "",
      topSelling: product.topSelling || false,
      featured: product.featured || false,
    });
    setDetail(null);
    setIsDialogOpen(true);

    setDetailLoading(true);
    try {
      const d = await fetchListingBySlug(product.slug);
      setDetail(d);
      setFormData((prev) => ({
        ...prev,
        webDescription: d.webDescription ?? "",
      }));
    } catch {
      // Non-critical
    } finally {
      setDetailLoading(false);
    }
  };

  const handleSave = async () => {
    if (!editingProduct) return;
    setSaveError("");

    updateMutation.mutate({
      slug: editingProduct.slug,
      data: {
        webDescription: formData.webDescription,
        topSelling: formData.topSelling,
        featured: formData.featured,
      },
    });
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !editingProduct) return;
    e.target.value = "";

    setUploading(true);
    try {
      await uploadMutation.mutateAsync({
        slug: editingProduct.slug,
        file,
      });
    } finally {
      setUploading(false);
    }
  };

  const handleRemoveImage = async (url: string) => {
    if (!editingProduct) return;
    await deleteImageMutation.mutateAsync({ slug: editingProduct.slug, url });
  };

  const displayPrice = (item: ListingVariant) => {
    return `${item.minPrice.toFixed(2)} TJS`;
  };

  const computeStock = (item: ListingVariant) => {
    return item.skus.reduce((acc, s) => acc + s.stockQuantity, 0);
  };

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
        <Button className="gap-1.5" onClick={() => setIsCreateOpen(true)}>
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
                      <p className="font-medium text-sm">
                        {item.colorDisplayName}
                      </p>
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
                      <span className="text-sm">
                        {displayPrice(item)}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <Lock className="h-3 w-3 text-muted-foreground" />
                      <span className="text-sm">{computeStock(item)}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {item.topSelling && (
                        <Badge variant="success">{t("topSeller")}</Badge>
                      )}
                      {item.featured && (
                        <Badge variant="default">
                          <Star className="h-3 w-3 mr-0.5" />
                          {t("featured")}
                        </Badge>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right pr-4">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleOpenDialog(item)}
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

      {/* Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="max-w-2xl px-8">
          <DialogHeader>
            <DialogTitle>{t("editProductTitle")}</DialogTitle>
            <DialogDescription>
              {t("editProductDesc")}
            </DialogDescription>
          </DialogHeader>

          {editingProduct && (
            <div className="space-y-5 py-2 max-h-[70vh] overflow-y-auto pr-1">
              {/* ── Catalog Data (Read-Only) ─────────────────────── */}
              <div>
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3 flex items-center gap-1.5">
                  <Lock className="h-3 w-3" />
                  {t("erpSyncedData")}
                </p>
                <div className="space-y-2">
                  <div className="space-y-1">
                    <label className="text-xs text-muted-foreground">{t("slug")}</label>
                    <Input
                      disabled
                      value={editingProduct.slug}
                      className="bg-slate-50 dark:bg-slate-900 h-8 text-sm"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-xs text-muted-foreground">{t("nameAndColor")}</label>
                    <Input
                      disabled
                      value={`${editingProduct.colorDisplayName} — ${editingProduct.colorKey}`}
                      className="bg-slate-50 dark:bg-slate-900 h-8 text-sm"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div className="space-y-1">
                      <label className="text-xs text-muted-foreground">{t("price")}</label>
                      <Input
                        disabled
                        value={displayPrice(editingProduct)}
                        className="bg-slate-50 dark:bg-slate-900 h-8 text-sm"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs text-muted-foreground">{t("stock")}</label>
                      <Input
                        disabled
                        value={String(computeStock(editingProduct))}
                        className="bg-slate-50 dark:bg-slate-900 h-8 text-sm"
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* ── SKU Breakdown ─────────────────────────────────── */}
              <div className="border-t pt-4">
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 flex items-center gap-1.5">
                  {!isAdmin && <Lock className="h-3 w-3" />}
                  {t("sizesAndStock")}
                  {isAdmin && (
                    <span className="ml-1 text-xs font-normal text-muted-foreground normal-case tracking-normal">
                      — {t("adminEditable")}
                    </span>
                  )}
                </p>
                {detailLoading ? (
                  <div className="space-y-1.5">
                    {Array.from({ length: 3 }).map((_, i) => (
                      <Skeleton key={i} className="h-6 w-full" />
                    ))}
                  </div>
                ) : detail?.skus && detail.skus.length > 0 ? (
                  <div className="rounded-md border text-sm">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b bg-muted/50">
                          <th className="text-left px-3 py-1.5 text-xs font-medium text-muted-foreground">{t("size")}</th>
                          <th className={`px-3 py-1.5 text-xs font-medium text-muted-foreground ${isAdmin ? "text-left" : "text-right"}`}>{t("price")}</th>
                          <th className={`px-3 py-1.5 text-xs font-medium text-muted-foreground ${isAdmin ? "text-left" : "text-right"}`}>{t("stock")}</th>
                          {isAdmin && <th className="w-16 px-2 py-1.5" />}
                        </tr>
                      </thead>
                      <tbody>
                        {detail.skus.map((sku) => (
                          <tr key={sku.skuId} className="border-b last:border-0">
                            <td className="px-3 py-1.5 font-medium">{sku.sizeLabel}</td>
                            {isAdmin ? (
                              <>
                                <td className="px-2 py-1.5">
                                  <Input
                                    type="number"
                                    min={0}
                                    step={0.01}
                                    value={pendingPrices[sku.skuId] ?? sku.price.toFixed(2)}
                                    onChange={(e) =>
                                      setPendingPrices((prev) => ({ ...prev, [sku.skuId]: e.target.value }))
                                    }
                                    onBlur={() => handleSkuPriceBlur(sku.skuId)}
                                    className="h-7 w-28 text-sm"
                                  />
                                </td>
                                <td className="px-2 py-1.5">
                                  <Input
                                    type="number"
                                    min={0}
                                    value={pendingStocks[sku.skuId] ?? sku.stockQuantity}
                                    onChange={(e) =>
                                      setPendingStocks((prev) => ({ ...prev, [sku.skuId]: e.target.value }))
                                    }
                                    className="h-7 w-20 text-sm"
                                  />
                                </td>
                                <td className="px-2 py-1.5">
                                  {pendingStocks[sku.skuId] !== undefined && (
                                    <Button
                                      size="sm"
                                      variant="outline"
                                      className="h-7 px-2 text-xs"
                                      disabled={skuStockMutation.isPending}
                                      onClick={() => handleSkuStockSave(sku.skuId)}
                                    >
                                      {skuStockMutation.isPending ? (
                                        <Loader2 className="h-3 w-3 animate-spin" />
                                      ) : (
                                        <Check className="h-3 w-3" />
                                      )}
                                    </Button>
                                  )}
                                </td>
                              </>
                            ) : (
                              <>
                                <td className="px-3 py-1.5 text-right text-muted-foreground">{sku.price.toFixed(2)} TJS</td>
                                <td className={`px-3 py-1.5 text-right font-medium ${sku.stockQuantity === 0 ? "text-destructive" : ""}`}>
                                  {sku.stockQuantity}
                                </td>
                              </>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">{t("noSkuData")}</p>
                )}
              </div>

              {/* ── Enrichment (Editable) ─────────────────────────── */}
              <div className="border-t pt-4">
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
                  {t("enrichment")}
                </p>
                <div className="space-y-3">
                  <div className="space-y-1">
                    <label className="text-sm font-medium">{t("webDescription")}</label>
                    <textarea
                      value={formData.webDescription}
                      onChange={(e) =>
                        setFormData({ ...formData, webDescription: e.target.value })
                      }
                      className="flex min-h-[80px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                      rows={3}
                    />
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        id="topSelling"
                        checked={formData.topSelling}
                        onChange={(e) =>
                          setFormData({ ...formData, topSelling: e.target.checked })
                        }
                        className="h-4 w-4 rounded border-input"
                      />
                      <label htmlFor="topSelling" className="text-sm">
                        {t("topSelling")}
                      </label>
                    </div>
                    <div className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        id="featured"
                        checked={formData.featured}
                        onChange={(e) =>
                          setFormData({ ...formData, featured: e.target.checked })
                        }
                        className="h-4 w-4 rounded border-input"
                      />
                      <label htmlFor="featured" className="text-sm flex items-center gap-1">
                        <Star className="h-3.5 w-3.5" />
                        {t("featured")}
                      </label>
                    </div>
                  </div>
                </div>
              </div>

              {/* ── Image Management ──────────────────────────────── */}
              <div className="border-t pt-4">
                <div className="flex items-center justify-between mb-3">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                    {t("productImages")}
                  </p>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="gap-1.5"
                    disabled={uploading}
                    onClick={() => fileInputRef.current?.click()}
                  >
                    {uploading ? (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    ) : (
                      <Upload className="h-3.5 w-3.5" />
                    )}
                    {uploading ? t("uploading") : t("addImage")}
                  </Button>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={handleImageUpload}
                  />
                </div>

                <div className="grid grid-cols-3 gap-3 relative">
                  {uploading && (
                    <div className="absolute inset-0 bg-background/60 z-10 flex items-center justify-center rounded-md">
                      <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                    </div>
                  )}
                  {editingProduct.images.length > 0 ? (
                    editingProduct.images.map((url, idx) => (
                      <div
                        key={url}
                        className="group relative aspect-square rounded-md border overflow-hidden bg-muted"
                      >
                        <Image
                          src={url}
                          alt="Product"
                          fill
                          className="object-cover"
                          unoptimized
                        />
                        {idx === 0 && (
                          <span className="absolute bottom-1 left-1 z-10 rounded bg-black/60 px-1.5 py-0.5 text-[10px] font-medium text-white">
                            {t("primary")}
                          </span>
                        )}
                        <button
                          type="button"
                          onClick={() => handleRemoveImage(url)}
                          aria-label="Remove image"
                          className="absolute top-1 right-1 z-10 rounded-full bg-destructive p-1 text-destructive-foreground opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </div>
                    ))
                  ) : (
                    <div className="col-span-3 flex flex-col items-center justify-center rounded-md border border-dashed py-6 text-muted-foreground">
                      <Package className="h-8 w-8 mb-2" />
                      <p className="text-sm">{t("noImagesYet")}</p>
                    </div>
                  )}
                </div>
              </div>

              {saveError && (
                <div className="flex items-center gap-2 text-destructive text-sm">
                  <AlertCircle className="h-4 w-4" />
                  {saveError}
                </div>
              )}
            </div>
          )}

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsDialogOpen(false)}
            >
              {t("cancel")}
            </Button>
            <Button onClick={handleSave} disabled={updateMutation.isPending}>
              {updateMutation.isPending ? t("saving") : t("save")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <CreateProductDialog
        open={isCreateOpen}
        onOpenChange={setIsCreateOpen}
      />
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
