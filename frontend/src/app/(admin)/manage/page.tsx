"use client";

import { useState, useEffect } from "react";
import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { useAuth } from "@/features/auth";
import {
  getProducts,
  createProduct,
  updateProduct,
  deleteProduct,
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
import { Plus, Pencil, Trash2, Lock, AlertCircle } from "lucide-react";

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

  // Form State
  const [formData, setFormData] = useState({
    erpId: "",
    name: "",
    price: "",
    stock: "",
    webDescription: "",
    topSelling: false,
  });

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
        images: [],
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
                    <TableHead className="pl-4">Product</TableHead>
                    <TableHead>ERP ID</TableHead>
                    <TableHead>Price</TableHead>
                    <TableHead>Stock</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right pr-4">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {products.map((product) => (
                    <TableRow key={product.id}>
                      <TableCell className="pl-4">
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
            {products.length === 0 && !loading && (
              <div className="p-12 text-center text-muted-foreground">
                No products found. Start by adding one.
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

            <div className="space-y-4 py-2">
              <div className="space-y-2">
                <label className="text-sm font-medium">ERP ID</label>
                <Input
                  disabled={!!editingProduct}
                  value={formData.erpId}
                  onChange={(e) =>
                    setFormData({ ...formData, erpId: e.target.value })
                  }
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
                  value={formData.name}
                  onChange={(e) =>
                    setFormData({ ...formData, name: e.target.value })
                  }
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
                    value={formData.price}
                    onChange={(e) =>
                      setFormData({ ...formData, price: e.target.value })
                    }
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium flex items-center gap-1.5">
                    Stock
                    <Lock className="h-3 w-3 text-muted-foreground" />
                  </label>
                  <Input
                    type="number"
                    value={formData.stock}
                    onChange={(e) =>
                      setFormData({ ...formData, stock: e.target.value })
                    }
                  />
                </div>
              </div>
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
