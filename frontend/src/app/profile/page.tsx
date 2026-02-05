"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { useAuth } from "@/features/auth";
import { useState, useEffect } from "react";
import { getMyOrders, updateProfile } from "@/features/profile/api";
import { Order } from "@/features/profile/types";

export default function ProfilePage() {
    const { user, updateUser } = useAuth();

    // --- State ---
    const [isEditing, setIsEditing] = useState(false);
    const [showOrders, setShowOrders] = useState(false);
    const [orders, setOrders] = useState<Order[]>([]);

    // Edit Form
    const [formData, setFormData] = useState({ name: "", email: "" });
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState("");

    // Order History
    const [loadingOrders, setLoadingOrders] = useState(false);

    // Initialize form data when user loads
    useEffect(() => {
        if (user) {
            setFormData({
                name: user.name || "",
                email: user.email || ""
            });
        }
    }, [user]);

    // Handlers
    const handleSaveProfile = async () => {
        setSaving(true);
        setSaveError("");
        try {
            const updatedUser = await updateProfile(formData);
            updateUser(updatedUser);
            setIsEditing(false);
        } catch (err: any) {
            console.error(err);
            setSaveError("Failed to update profile. " + (err.response?.data?.message || err.message));
        } finally {
            setSaving(false);
        }
    };

    const handleViewOrders = async () => {
        if (showOrders) {
            setShowOrders(false);
            return;
        }

        setShowOrders(true);
        setLoadingOrders(true);
        try {
            const data = await getMyOrders();
            setOrders(data);
        } catch (err) {
            console.error(err);
        } finally {
            setLoadingOrders(false);
        }
    };

    return (
        <ProtectedRoute>
            <div className="min-h-screen bg-gray-50 py-12">
                <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
                    {/* Header */}
                    <div className="bg-white shadow rounded-lg mb-6">
                        <div className="px-6 py-8 sm:px-8">
                            <div className="flex items-center justify-between">
                                <div>
                                    <h1 className="text-3xl font-bold text-gray-900">My Profile</h1>
                                    <p className="mt-1 text-sm text-gray-500">View and manage your account information</p>
                                </div>
                                {/* Role Badge */}
                                <div className={`px-4 py-2 rounded-full text-sm font-semibold ${user?.role === "MANAGER" ? "bg-purple-100 text-purple-700" : "bg-blue-100 text-blue-700"}`}>
                                    {user?.role}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Account Information / Edit Form */}
                    <div className="bg-white shadow rounded-lg mb-6">
                        <div className="px-6 py-6 sm:px-8">
                            <div className="flex justify-between items-center mb-4">
                                <h2 className="text-xl font-semibold text-gray-900">Account Information</h2>

                                {!isEditing && (
                                    <button
                                        onClick={() => setIsEditing(true)}
                                        className="text-indigo-600 hover:text-indigo-800 text-sm font-medium"
                                    >
                                        Edit
                                    </button>
                                )}
                            </div>

                            {isEditing ? (
                                <div className="space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700">Name</label>
                                        <input
                                            type="text"
                                            value={formData.name}
                                            onChange={e => setFormData({ ...formData, name: e.target.value })}
                                            className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700">Email</label>
                                        <input
                                            type="email"
                                            value={formData.email}
                                            onChange={e => setFormData({ ...formData, email: e.target.value })}
                                            className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                                        />
                                    </div>
                                    {saveError && <p className="text-red-500 text-sm">{saveError}</p>}
                                    <div className="flex gap-3">
                                        <button
                                            onClick={handleSaveProfile}
                                            disabled={saving}
                                            className="bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700 text-sm disabled:opacity-50"
                                        >
                                            {saving ? "Saving..." : "Save Changes"}
                                        </button>
                                        <button
                                            onClick={() => setIsEditing(false)}
                                            className="text-gray-600 hover:text-gray-800 px-4 py-2 text-sm"
                                        >
                                            Cancel
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-500">Phone Number</label>
                                        <p className="mt-1 text-lg text-gray-900">{user?.phone}</p>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-500">Name</label>
                                        <p className="mt-1 text-lg text-gray-900">{user?.name || "Not set"}</p>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-500">Email</label>
                                        <p className="mt-1 text-lg text-gray-900">{user?.email || "Not set"}</p>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-500">User ID</label>
                                        <p className="mt-1 text-lg text-gray-900">{user?.id}</p>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Order History */}
                    <div className="bg-white shadow rounded-lg mb-6">
                        <div className="px-6 py-6 sm:px-8">
                            <div className="flex justify-between items-center mb-4">
                                <h2 className="text-xl font-semibold text-gray-900">Order History</h2>
                                <button
                                    onClick={handleViewOrders}
                                    className="text-indigo-600 hover:text-indigo-800 text-sm font-medium"
                                >
                                    {showOrders ? "Hide Orders" : "View Orders"}
                                </button>
                            </div>

                            {showOrders && (
                                <div className="mt-4">
                                    {loadingOrders ? (
                                        <p className="text-gray-500">Loading orders...</p>
                                    ) : orders.length === 0 ? (
                                        <p className="text-gray-500">No orders found.</p>
                                    ) : (
                                        <div className="space-y-4">
                                            {orders.map(order => (
                                                <div key={order.id} className="border rounded-lg p-4">
                                                    <div className="flex justify-between items-center mb-2">
                                                        <div>
                                                            <span className="font-medium">Order #{order.id}</span>
                                                            <span className={`ml-3 text-xs px-2 py-1 rounded-full ${order.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                                                                    order.status === 'DELIVERED' ? 'bg-green-100 text-green-800' :
                                                                        'bg-gray-100 text-gray-800'
                                                                }`}>
                                                                {order.status}
                                                            </span>
                                                        </div>
                                                        <span className="text-sm text-gray-500">
                                                            {new Date(order.createdAt).toLocaleDateString()}
                                                        </span>
                                                    </div>
                                                    <div className="text-sm text-gray-600 mb-2">
                                                        {order.items.map(i => `${i.productName} (x${i.quantity})`).join(", ")}
                                                    </div>
                                                    <div className="font-semibold text-right">
                                                        Total: ${order.totalAmount}
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Loyalty Points (Placeholder) */}
                    <div className="bg-white shadow rounded-lg mb-6">
                        <div className="px-6 py-6 sm:px-8">
                            <h2 className="text-xl font-semibold text-gray-900 mb-4">Loyalty Points</h2>
                            <div className="bg-gradient-to-r from-indigo-500 to-purple-600 rounded-lg p-6 text-white">
                                <div className="flex items-center justify-between">
                                    <div>
                                        <p className="text-sm opacity-90">Available Points</p>
                                        <p className="text-4xl font-bold mt-1">0</p>
                                        <p className="text-xs opacity-75 mt-2">Coming soon - Earn points with every purchase!</p>
                                    </div>
                                    <div>
                                        <svg className="w-16 h-16 opacity-75" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </ProtectedRoute>
    );
}
