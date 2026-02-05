"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { useAuth } from "@/features/auth";
import { useState, useEffect } from "react";
import { getProducts, createProduct, updateProduct, deleteProduct } from "@/features/products/api";
import { Product } from "@/features/products/types";

export default function ManageProductsPage() {
    const { user } = useAuth();

    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(false);

    // Modal State
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingProduct, setEditingProduct] = useState<Product | null>(null);
    const [saving, setSaving] = useState(false);

    // Form State
    const [formData, setFormData] = useState({
        erpId: "",
        name: "",
        price: "",
        stock: "",
        webDescription: "",
        topSelling: false
    });

    useEffect(() => {
        loadProducts();
    }, [page]);

    const loadProducts = async () => {
        setLoading(true);
        try {
            const data = await getProducts(page, 100); // larger limit for now
            setProducts(data.products);
            setHasMore(data.hasMore);
        } catch (e) {
            console.error("Failed to load products", e);
        } finally {
            setLoading(false);
        }
    };

    const handleOpenModal = (product?: Product) => {
        if (product) {
            setEditingProduct(product);
            setFormData({
                erpId: product.erpId,
                name: product.name || "",
                price: product.price?.toString() || "",
                stock: product.stock?.toString() || "",
                webDescription: product.webDescription || "",
                topSelling: product.topSelling
            });
        } else {
            setEditingProduct(null);
            setFormData({
                erpId: `SKU-${Date.now().toString().slice(-6)}`,
                name: "",
                price: "",
                stock: "",
                webDescription: "",
                topSelling: false
            });
        }
        setIsModalOpen(true);
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            const payload = {
                erpId: formData.erpId,
                name: formData.name,
                price: parseFloat(formData.price) || 0,
                stock: parseInt(formData.stock) || 0,
                webDescription: formData.webDescription,
                topSelling: formData.topSelling,
                images: [] // TODO: Image upload separate
            };

            if (editingProduct) {
                // For update, exclude ERP ID if it's path param, but we might send body
                await updateProduct(editingProduct.erpId, payload);
            } else {
                await createProduct(payload);
            }
            setIsModalOpen(false);
            loadProducts();
        } catch (e) {
            console.error(e);
            alert("Failed to save product");
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (erpId: string) => {
        if (!window.confirm("Are you sure you want to delete this product?")) return;
        try {
            await deleteProduct(erpId);
            loadProducts();
        } catch (e) {
            console.error(e);
            alert("Failed to delete product");
        }
    };

    return (
        <ProtectedRoute requiredRole="MANAGER">
            <div className="min-h-screen bg-gray-50 py-12">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    {/* Header */}
                    <div className="mb-8 flex flex-col sm:flex-row sm:items-center sm:justify-between">
                        <div>
                            <h1 className="text-3xl font-bold text-gray-900">Product Management</h1>
                            <p className="mt-1 text-sm text-gray-500">Manage your product catalog, pricing, and inventory</p>
                        </div>
                        <div className="mt-4 sm:mt-0 flex gap-3">
                            <button
                                onClick={() => handleOpenModal()}
                                className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 transition-colors shadow-sm"
                            >
                                + Add New Product
                            </button>
                        </div>
                    </div>

                    {/* Product List */}
                    <div className="bg-white shadow rounded-lg overflow-hidden">
                        {loading && products.length === 0 ? (
                            <div className="p-12 text-center text-gray-500">Loading products...</div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="min-w-full divide-y divide-gray-200">
                                    <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Product</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ERP ID</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Price</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Stock</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody className="bg-white divide-y divide-gray-200">
                                        {products.map((product) => (
                                            <tr key={product.id}>
                                                <td className="px-6 py-4 whitespace-nowrap">
                                                    <div className="text-sm font-medium text-gray-900">{product.name || "Unnamed Product"}</div>
                                                    <div className="text-sm text-gray-500 truncate max-w-xs">{product.webDescription}</div>
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                    {product.erpId}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                                    ${product.price}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                                    {product.stock}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap">
                                                    {product.topSelling && (
                                                        <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                                                            Top Selling
                                                        </span>
                                                    )}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                                    <button
                                                        onClick={() => handleOpenModal(product)}
                                                        className="text-indigo-600 hover:text-indigo-900 mr-4"
                                                    >
                                                        Edit
                                                    </button>
                                                    <button
                                                        onClick={() => handleDelete(product.erpId)}
                                                        className="text-red-600 hover:text-red-900"
                                                    >
                                                        Delete
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                        {products.length === 0 && !loading && (
                            <div className="p-12 text-center text-gray-500">No products found. Start by adding one.</div>
                        )}
                    </div>
                </div>

                {/* Modal */}
                {isModalOpen && (
                    <div className="fixed inset-0 z-50 overflow-y-auto">
                        <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
                            <div className="fixed inset-0 transition-opacity" aria-hidden="true" onClick={() => setIsModalOpen(false)}>
                                <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
                            </div>
                            <span className="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>

                            <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
                                <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                                    <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
                                        {editingProduct ? "Edit Product" : "Add New Product"}
                                    </h3>
                                    <div className="space-y-4">
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700">ERP ID</label>
                                            <input
                                                type="text"
                                                disabled={!!editingProduct}
                                                value={formData.erpId}
                                                onChange={e => setFormData({ ...formData, erpId: e.target.value })}
                                                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 disabled:bg-gray-100"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700">Name</label>
                                            <input
                                                type="text"
                                                value={formData.name}
                                                onChange={e => setFormData({ ...formData, name: e.target.value })}
                                                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3"
                                            />
                                        </div>
                                        <div className="grid grid-cols-2 gap-4">
                                            <div>
                                                <label className="block text-sm font-medium text-gray-700">Price</label>
                                                <input
                                                    type="number"
                                                    value={formData.price}
                                                    onChange={e => setFormData({ ...formData, price: e.target.value })}
                                                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3"
                                                />
                                            </div>
                                            <div>
                                                <label className="block text-sm font-medium text-gray-700">Stock</label>
                                                <input
                                                    type="number"
                                                    value={formData.stock}
                                                    onChange={e => setFormData({ ...formData, stock: e.target.value })}
                                                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3"
                                                />
                                            </div>
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700">Description</label>
                                            <textarea
                                                value={formData.webDescription}
                                                onChange={e => setFormData({ ...formData, webDescription: e.target.value })}
                                                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3"
                                                rows={3}
                                            />
                                        </div>
                                        <div className="flex items-center">
                                            <input
                                                type="checkbox"
                                                checked={formData.topSelling}
                                                onChange={e => setFormData({ ...formData, topSelling: e.target.checked })}
                                                className="h-4 w-4 text-indigo-600 border-gray-300 rounded"
                                            />
                                            <label className="ml-2 block text-sm text-gray-900">
                                                Top Selling Product
                                            </label>
                                        </div>
                                    </div>
                                </div>
                                <div className="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                                    <button
                                        type="button"
                                        disabled={saving}
                                        onClick={handleSave}
                                        className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50"
                                    >
                                        {saving ? "Saving..." : "Save"}
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => setIsModalOpen(false)}
                                        className="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm"
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </ProtectedRoute>
    );
}
