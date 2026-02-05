"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { useAuth } from "@/features/auth";
import Link from "next/link";

/**
 * Product management page for MANAGER users.
 * 
 * Provides UI for managing products, inventory, and pricing.
 * Protected route - requires MANAGER role.
 */
export default function ManageProductsPage() {
    const { user } = useAuth();

    return (
        <ProtectedRoute requiredRole="MANAGER">
            <div className="min-h-screen bg-gray-50 py-12">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    {/* Header */}
                    <div className="mb-8">
                        <div className="flex items-center justify-between">
                            <div>
                                <h1 className="text-3xl font-bold text-gray-900">
                                    Product Management
                                </h1>
                                <p className="mt-1 text-sm text-gray-500">
                                    Manage your product catalog, pricing, and inventory
                                </p>
                            </div>
                            <div className="flex items-center gap-3">
                                <span className="px-3 py-1 bg-purple-100 text-purple-700 rounded-full text-sm font-semibold">
                                    Manager Access
                                </span>
                                <button
                                    disabled
                                    className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    Add New Product
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Stats Overview */}
                    <div className="grid grid-cols-1 gap-6 sm:grid-cols-3 mb-8">
                        <div className="bg-white shadow rounded-lg p-6">
                            <div className="flex items-center">
                                <div className="flex-shrink-0 bg-indigo-100 rounded-md p-3">
                                    <svg
                                        className="h-6 w-6 text-indigo-600"
                                        fill="none"
                                        stroke="currentColor"
                                        viewBox="0 0 24 24"
                                    >
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            strokeWidth={2}
                                            d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
                                        />
                                    </svg>
                                </div>
                                <div className="ml-5">
                                    <p className="text-sm font-medium text-gray-500">
                                        Total Products
                                    </p>
                                    <p className="text-2xl font-semibold text-gray-900">—</p>
                                </div>
                            </div>
                        </div>

                        <div className="bg-white shadow rounded-lg p-6">
                            <div className="flex items-center">
                                <div className="flex-shrink-0 bg-green-100 rounded-md p-3">
                                    <svg
                                        className="h-6 w-6 text-green-600"
                                        fill="none"
                                        stroke="currentColor"
                                        viewBox="0 0 24 24"
                                    >
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            strokeWidth={2}
                                            d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                                        />
                                    </svg>
                                </div>
                                <div className="ml-5">
                                    <p className="text-sm font-medium text-gray-500">In Stock</p>
                                    <p className="text-2xl font-semibold text-gray-900">—</p>
                                </div>
                            </div>
                        </div>

                        <div className="bg-white shadow rounded-lg p-6">
                            <div className="flex items-center">
                                <div className="flex-shrink-0 bg-yellow-100 rounded-md p-3">
                                    <svg
                                        className="h-6 w-6 text-yellow-600"
                                        fill="none"
                                        stroke="currentColor"
                                        viewBox="0 0 24 24"
                                    >
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            strokeWidth={2}
                                            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                                        />
                                    </svg>
                                </div>
                                <div className="ml-5">
                                    <p className="text-sm font-medium text-gray-500">Low Stock</p>
                                    <p className="text-2xl font-semibold text-gray-900">—</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Product Management Interface */}
                    <div className="bg-white shadow rounded-lg">
                        <div className="px-6 py-6 sm:px-8">
                            <h2 className="text-xl font-semibold text-gray-900 mb-4">
                                Product Catalog
                            </h2>

                            {/* Coming Soon Message */}
                            <div className="text-center py-12 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300">
                                <svg
                                    className="mx-auto h-12 w-12 text-gray-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
                                    />
                                </svg>
                                <h3 className="mt-2 text-sm font-medium text-gray-900">
                                    Product Management Coming Soon
                                </h3>
                                <p className="mt-1 text-sm text-gray-500">
                                    Full CRUD operations for products will be available in the next release.
                                </p>
                                <div className="mt-6">
                                    <Link
                                        href="/products"
                                        className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
                                    >
                                        View Product Catalog
                                    </Link>
                                </div>
                            </div>

                            {/* Feature List */}
                            <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2">
                                <div className="border border-gray-200 rounded-lg p-4">
                                    <h3 className="font-medium text-gray-900 mb-2">
                                        ✨ Planned Features
                                    </h3>
                                    <ul className="text-sm text-gray-600 space-y-1">
                                        <li>• Add/Edit/Delete Products</li>
                                        <li>• Bulk Import from ERP</li>
                                        <li>• Price Management</li>
                                        <li>• Inventory Tracking</li>
                                    </ul>
                                </div>
                                <div className="border border-gray-200 rounded-lg p-4">
                                    <h3 className="font-medium text-gray-900 mb-2">
                                        🔒 Manager Permissions
                                    </h3>
                                    <ul className="text-sm text-gray-600 space-y-1">
                                        <li>• Update product details</li>
                                        <li>• Manage pricing tiers</li>
                                        <li>• View sales analytics</li>
                                        <li>• Export reports</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </ProtectedRoute>
    );
}
