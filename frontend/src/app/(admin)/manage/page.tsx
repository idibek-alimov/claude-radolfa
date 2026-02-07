"use client";

import { useState, useEffect, useMemo, useCallback, useRef } from "react";
import Image from "next/image";
import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { useAuth } from "@/features/auth";
import { toast } from "sonner";
import {
  getProducts,
  createProduct,
  updateProduct,
  deleteProduct,
  uploadProductImage,
} from "@/features/products/api";
import { Product } from "@/features/products/types";
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
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogAction,
  AlertDialogCancel,
} from "@/shared/ui/alert-dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Badge } from "@/shared/ui/badge";
import { Skeleton } from "@/shared/ui/skeleton";
import { Plus, Pencil, Trash2, Lock, AlertCircle, Search, Package, Upload, Loader2, X } from "lucide-react";

export default function ManageProductsPage() {
  const { user } = useAuth();

  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);

  // Dialog State
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");

  // Delete confirmation
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  // Search State
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const debounceRef = useRef<NodeJS.Timeout>();

  const handleSearchChange = useCallback((value: string) => {
    setSearchQuery(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim().toLowerCase());
    }, 300);
  }, []);

  const filteredProducts = useMemo(() => {
    if (!debouncedSearch) return products;
    return products.filter(
      (p) =>
        p.name?.toLowerCase().includes(debouncedSearch) ||
        p.erpId.toLowerCase().includes(debouncedSearch) ||
        p.webDescription?.toLowerCase().includes(debouncedSearch)
    );
  }, [products, debouncedSearch]);

  // Form State
  const [formData, setFormData] = useState({
    erpId: "",
    name: "",
    price: "",
    stock: "",
    webDescription: "",
    topSelling: false,
    images: [] as string[],
  });

  // Image upload
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadProducts();
  }, [page]);

  const loadProducts = async () => {
    setLoading(true);
    try {
      const data = await getProducts(page, 100);
      setProducts(data.products);
      setHasMore(data.hasMore);
    } catch (e) {
      console.error("Failed to load products", e);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (product?: Product) => {
    setSaveError("");
    if (product) {
      setEditingProduct(product);
      setFormData({
        erpId: product.erpId,
        name: product.name || "",
        price: product.price?.toString() || "",
        stock: product.stock?.toString() || "",
        webDescription: product.webDescription || "",
        topSelling: product.topSelling,
        images: product.images ?? [],
      });
    } else {
      setEditingProduct(null);
      setFormData({
        erpId: `SKU-${Date.now().toString().slice(-6)}`,
        name: "",
        price: "",
        stock: "",
        webDescription: "",
        topSelling: false,
        images: [],
      });
    }
    setIsDialogOpen(true);
  };

  const handleSave = async () => {
    setSaving(true);
    setSaveError("");
    try {
      const payload = {
        erpId: formData.erpId,
        name: formData.name,
        price: parseFloat(formData.price) || 0,
        stock: parseInt(formData.stock) || 0,
        webDescription: formData.webDescription,
        topSelling: formData.topSelling,
        images: formData.images,
      };

      if (editingProduct) {
        await updateProduct(editingProduct.erpId, payload);
      } else {
        await createProduct(payload);
      }
      setIsDialogOpen(false);
      loadProducts();
    } catch (e: any) {
      console.error(e);
      setSaveError(e.response?.data?.message || "Failed to save product");
    } finally {
      setSaving(false);
    }
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !editingProduct) return;
    // Reset input so the same file can be re-selected
    e.target.value = "";

    setUploading(true);
    try {
      const result = await uploadProductImage(editingProduct.erpId, file);
      setFormData((prev) => ({ ...prev, images: result.images }));
      // Update the product in the global list
      setProducts((prev) =>
        prev.map((p) =>
          p.erpId === editingProduct.erpId ? { ...p, images: result.images } : p
        )
      );
      toast.success("Image uploaded");
    } catch (err: any) {
      toast.error(err.message || "Upload failed");
    } finally {
      setUploading(false);
    }
  };

  const handleRemoveImage = async (url: string) => {
    if (!editingProduct) return;
    const updated = formData.images.filter((img) => img !== url);
    setFormData((prev) => ({ ...prev, images: updated }));
    try {
      await updateProduct(editingProduct.erpId, {
        webDescription: formData.webDescription,
        topSelling: formData.topSelling,
        images: updated,
      });
      setProducts((prev) =>
        prev.map((p) =>
          p.erpId === editingProduct.erpId ? { ...p, images: updated } : p
        )
      );
      toast.success("Image removed");
    } catch (err: any) {
      // Rollback
      setFormData((prev) => ({ ...prev, images: formData.images }));
      toast.error("Failed to remove image");
    }
  };

  const handleConfirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteProduct(deleteTarget);
      setDeleteTarget(null);
      loadProducts();
    } catch (e: any) {
      console.error(e);
      setSaveError(e.response?.data?.message || "Failed to delete product");
      setDeleteTarget(null);
    }
  };

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
                Manage your product catalog, pricing, and inventory
              </p>
            </div>
            <Button onClick={() => handleOpenDialog()} className="gap-1.5">
              <Plus className="h-4 w-4" />
              Add Product
            </Button>
          </div>

          {/* Search */}
          <div className="mb-4 relative max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              value={searchQuery}
              onChange={(e) => handleSearchChange(e.target.value)}
              placeholder="Search by name, ERP ID..."
              className="pl-9"
            />
          </div>

          {/* Product Table */}
          <div className="bg-card rounded-xl border shadow-sm">
            {loading && products.length === 0 ? (
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
                    <TableHead>ERP ID</TableHead>
                    <TableHead>Price</TableHead>
                    <TableHead>Stock</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right pr-4">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredProducts.map((product) => (
                    <TableRow key={product.id}>
                      <TableCell className="pl-4">
                        {product.images?.[0] ? (
                          <div className="relative h-10 w-10 rounded-md border overflow-hidden">
                            <Image
                              src={product.images[0]}
                              alt={product.name ?? "Product"}
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
                            {product.name || "Unnamed Product"}
                          </p>
                          <p className="text-xs text-muted-foreground truncate max-w-xs">
                            {product.webDescription}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <code className="text-xs bg-muted px-1.5 py-0.5 rounded">
                          {product.erpId}
                        </code>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1">
                          <Lock className="h-3 w-3 text-muted-foreground" />
                          <span className="text-sm">${product.price}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1">
                          <Lock className="h-3 w-3 text-muted-foreground" />
                          <span className="text-sm">{product.stock}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        {product.topSelling && (
                          <Badge variant="success">Top Seller</Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-right pr-4">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleOpenDialog(product)}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-destructive hover:text-destructive"
                            onClick={() => setDeleteTarget(product.erpId)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
            {filteredProducts.length === 0 && !loading && (
              <div className="p-12 text-center text-muted-foreground">
                {debouncedSearch
                  ? `No products matching "${debouncedSearch}"`
                  : "No products found. Start by adding one."}
              </div>
            )}
          </div>
        </div>

        {/* Edit/Create Dialog */}
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>
                {editingProduct ? "Edit Product" : "Add New Product"}
              </DialogTitle>
              <DialogDescription>
                {editingProduct
                  ? "Update the product details below. ERP-locked fields are synced from the source system."
                  : "Fill in the details to create a new product."}
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 py-2 max-h-[70vh] overflow-y-auto pr-1">
              {/* ERP-Locked Fields */}
              <div className="space-y-2">
                <label className="text-sm font-medium flex items-center gap-1.5">
                  ERP ID
                  {editingProduct && <Lock className="h-3 w-3 text-muted-foreground" />}
                </label>
                <Input
                  disabled={!!editingProduct}
                  value={formData.erpId}
                  onChange={(e) =>
                    setFormData({ ...formData, erpId: e.target.value })
                  }
                  className={editingProduct ? "bg-slate-50 dark:bg-slate-900" : ""}
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium flex items-center gap-1.5">
                  Name
                  <Lock className="h-3 w-3 text-muted-foreground" />
                  <span className="text-xs text-muted-foreground font-normal">
                    Synced from ERP
                  </span>
                </label>
                <Input
                  disabled={!!editingProduct}
                  value={formData.name}
                  onChange={(e) =>
                    setFormData({ ...formData, name: e.target.value })
                  }
                  className={editingProduct ? "bg-slate-50 dark:bg-slate-900" : ""}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium flex items-center gap-1.5">
                    Price
                    <Lock className="h-3 w-3 text-muted-foreground" />
                  </label>
                  <Input
                    type="number"
                    disabled={!!editingProduct}
                    value={formData.price}
                    onChange={(e) =>
                      setFormData({ ...formData, price: e.target.value })
                    }
                    className={editingProduct ? "bg-slate-50 dark:bg-slate-900" : ""}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium flex items-center gap-1.5">
                    Stock
                    <Lock className="h-3 w-3 text-muted-foreground" />
                  </label>
                  <Input
                    type="number"
                    disabled={!!editingProduct}
                    value={formData.stock}
                    onChange={(e) =>
                      setFormData({ ...formData, stock: e.target.value })
                    }
                    className={editingProduct ? "bg-slate-50 dark:bg-slate-900" : ""}
                  />
                </div>
              </div>

              {/* Editable Fields — Manager can enrich */}
              <div className="space-y-2">
                <label className="text-sm font-medium">Description</label>
                <textarea
                  value={formData.webDescription}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      webDescription: e.target.value,
                    })
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

              {/* Image Management — only in edit mode */}
              {editingProduct && (
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

                  {formData.images.length > 0 ? (
                    <div className="grid grid-cols-3 gap-3 relative">
                      {uploading && (
                        <div className="absolute inset-0 bg-background/60 z-10 flex items-center justify-center rounded-md">
                          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                        </div>
                      )}
                      {formData.images.map((url) => (
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
                      ))}
                    </div>
                  ) : (
                    <div className="flex flex-col items-center justify-center rounded-md border border-dashed py-6 text-muted-foreground">
                      <Package className="h-8 w-8 mb-2" />
                      <p className="text-sm">No images yet</p>
                    </div>
                  )}
                </div>
              )}

              {saveError && (
                <div className="flex items-center gap-2 text-destructive text-sm">
                  <AlertCircle className="h-4 w-4" />
                  {saveError}
                </div>
              )}
            </div>

            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setIsDialogOpen(false)}
              >
                Cancel
              </Button>
              <Button onClick={handleSave} disabled={saving}>
                {saving ? "Saving..." : "Save"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Delete Confirmation */}
        <AlertDialog
          open={!!deleteTarget}
          onOpenChange={(open) => !open && setDeleteTarget(null)}
        >
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Delete Product</AlertDialogTitle>
              <AlertDialogDescription>
                Are you sure you want to delete this product? This action cannot
                be undone.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Cancel</AlertDialogCancel>
              <AlertDialogAction
                onClick={handleConfirmDelete}
                className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              >
                Delete
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </ProtectedRoute>
  );
}
