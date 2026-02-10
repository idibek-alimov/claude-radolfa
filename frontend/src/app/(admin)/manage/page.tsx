"use client";

import { useState, useMemo, useCallback, useRef } from "react";
import Image from "next/image";
import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { useAuth } from "@/features/auth";
import { toast } from "sonner";
import {
  useQuery,
  useMutation,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import {
  fetchListings,
  updateListing,
  uploadListingImage,
  removeListingImage,
  searchListings,
} from "@/entities/product/api"; // Ensure imports are correct
import { ListingVariant } from "@/entities/product/model/types";
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
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Badge } from "@/shared/ui/badge";
import { Skeleton } from "@/shared/ui/skeleton";
import { Pencil, Lock, AlertCircle, Search, Package, Upload, Loader2, X } from "lucide-react";

export default function ManageProductsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  // ── Query State ─────────────────────────────────────────────────
  const [page, setPage] = useState(1);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const debounceRef = useRef<NodeJS.Timeout>();

  const handleSearchChange = useCallback((value: string) => {
    setSearchQuery(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim());
      setPage(1); // Reset to page 1 on search
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

  const listings = data?.items ?? [];

  // ── Dialog State ────────────────────────────────────────────────
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<ListingVariant | null>(null);
  const [saveError, setSaveError] = useState("");

  // Form State
  const [formData, setFormData] = useState({
    webDescription: "",
    topSelling: false,
  });

  // Image upload state
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // ── Mutations ──────────────────────────────────────────────────
  const updateMutation = useMutation({
    mutationFn: ({ slug, data }: { slug: string; data: any }) =>
      updateListing(slug, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
      toast.success("Product updated");
      setIsDialogOpen(false);
    },
    onError: (err: any) => {
      setSaveError(err.message || "Failed to update");
    },
  });

  const uploadMutation = useMutation({
    mutationFn: ({ slug, file }: { slug: string; file: File }) =>
      uploadListingImage(slug, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
      toast.success("Image uploaded. Processing in background.");
      // Note: Image processing is async, so it might not appear immediately.
    },
    onError: (err: any) => toast.error(err.message || "Upload failed"),
  });

  const deleteImageMutation = useMutation({
    mutationFn: ({ slug, url }: { slug: string; url: string }) =>
      removeListingImage(slug, url),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
      toast.success("Image removed");
      // Optimistic update for standard image removal if synchronous
      if (editingProduct) {
        setEditingProduct({
          ...editingProduct,
          images: editingProduct.images.filter(url => url !== variables.url)
        });
      }
    },
    onError: (err: any) => toast.error(err.message || "Failed to remove image"),
  });

  // ── Handlers ────────────────────────────────────────────────────

  const handleOpenDialog = (product: ListingVariant) => {
    setSaveError("");
    setEditingProduct(product);
    setFormData({
      webDescription: product.webDescription || "",
      topSelling: product.topSelling || false,
    });
    setIsDialogOpen(true);
  };

  const handleSave = async () => {
    if (!editingProduct) return;
    setSaveError("");

    updateMutation.mutate({
      slug: editingProduct.slug,
      data: {
        webDescription: formData.webDescription,
        topSelling: formData.topSelling,
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

  // ── Helper ──────────────────────────────────────────────────────
  const formatPrice = (start: number, end: number) => {
    if (start === end) return `$${start.toFixed(2)}`;
    return `$${start.toFixed(2)} - $${end.toFixed(2)}`;
  };

  // ── Render ──────────────────────────────────────────────────────

  return (
    <ProtectedRoute requiredRole="MANAGER">
      <div className="min-h-screen bg-muted/30 py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="mb-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-foreground">
                Product Management
              </h1>
              <p className="mt-1 text-sm text-muted-foreground">
                Enrich product details. Pricing and stock are synced from ERP.
              </p>
            </div>
          </div>

          {/* Search */}
          <div className="mb-4 relative max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              value={searchQuery}
              onChange={(e) => handleSearchChange(e.target.value)}
              placeholder="Search by name, slug..."
              className="pl-9"
            />
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
                    <TableHead className="pl-4 w-[56px]">Image</TableHead>
                    <TableHead>Product</TableHead>
                    <TableHead>Slug / Key</TableHead>
                    <TableHead>Price</TableHead>
                    <TableHead>Stock</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right pr-4">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {listings.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="pl-4">
                        {item.images?.[0] ? (
                          <div className="relative h-10 w-10 rounded-md border overflow-hidden">
                            <Image
                              src={item.images[0]}
                              alt={item.name}
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
                            {item.name}
                          </p>
                          <p className="text-xs text-muted-foreground truncate max-w-xs">
                            {item.webDescription}
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
                            {formatPrice(item.priceStart, item.priceEnd)}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1">
                          <Lock className="h-3 w-3 text-muted-foreground" />
                          <span className="text-sm">{item.totalStock}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        {item.topSelling && (
                          <Badge variant="success">Top Seller</Badge>
                        )}
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
                  ? `No products matching "${debouncedSearch}"`
                  : "No products found."}
              </div>
            )}
          </div>
        </div>

        {/* Edit Dialog */}
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Edit Product</DialogTitle>
              <DialogDescription>
                Update description and images. Price, stock, and basic details are synced from ERP and cannot be changed here.
              </DialogDescription>
            </DialogHeader>

            {editingProduct && (
              <div className="space-y-4 py-2 max-h-[70vh] overflow-y-auto pr-1">
                {/* ERP-Locked Fields */}
                <div className="space-y-2">
                  <label className="text-sm font-medium flex items-center gap-1.5">
                    Slug
                    <Lock className="h-3 w-3 text-muted-foreground" />
                  </label>
                  <Input
                    disabled
                    value={editingProduct.slug}
                    className="bg-slate-50 dark:bg-slate-900"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium flex items-center gap-1.5">
                    Name (and Color)
                    <Lock className="h-3 w-3 text-muted-foreground" />
                  </label>
                  <Input
                    disabled
                    value={`${editingProduct.name} - ${editingProduct.colorKey}`}
                    className="bg-slate-50 dark:bg-slate-900"
                  />
                </div>

                {/* Editable Fields */}
                <div className="space-y-2">
                  <label className="text-sm font-medium">Web Description</label>
                  <textarea
                    value={formData.webDescription}
                    onChange={(e) =>
                      setFormData({ ...formData, webDescription: e.target.value })
                    }
                    className="flex min-h-[80px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    rows={3}
                  />
                </div>
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
                    Top Selling Product
                  </label>
                </div>

                {/* Image Management */}
                <div className="space-y-3 pt-2 border-t">
                  <div className="flex items-center justify-between">
                    <label className="text-sm font-medium">
                      Product Images
                    </label>
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
                      {uploading ? "Uploading..." : "Add Image"}
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
                      editingProduct.images.map((url) => (
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
                          <button
                            type="button"
                            onClick={() => handleRemoveImage(url)}
                            className="absolute top-1 right-1 z-10 rounded-full bg-destructive p-1 text-destructive-foreground opacity-0 group-hover:opacity-100 transition-opacity"
                          >
                            <X className="h-3 w-3" />
                          </button>
                        </div>
                      ))
                    ) : (
                      <div className="col-span-3 flex flex-col items-center justify-center rounded-md border border-dashed py-6 text-muted-foreground">
                        <Package className="h-8 w-8 mb-2" />
                        <p className="text-sm">No images yet</p>
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
                Cancel
              </Button>
              <Button onClick={handleSave} disabled={updateMutation.isPending}>
                {updateMutation.isPending ? "Saving..." : "Save"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </ProtectedRoute>
  );
}
