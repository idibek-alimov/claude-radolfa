"use client";

import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { useAuth } from "@/features/auth";

/**
 * User profile page.
 * 
 * Shows authenticated user's information including:
 * - Phone number
 * - Role
 * - Loyalty points (placeholder - backend doesn't support yet)
 */
export default function ProfilePage() {
    const { user } = useAuth();

    return (
        <ProtectedRoute>
            <div className="min-h-screen bg-gray-50 py-12">
                <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
                    {/* Header */}
                    <div className="bg-white shadow rounded-lg mb-6">
                        <div className="px-6 py-8 sm:px-8">
                            <div className="flex items-center justify-between">
                                <div>
                                    <h1 className="text-3xl font-bold text-gray-900">
                                        My Profile
                                    </h1>
                                    <p className="mt-1 text-sm text-gray-500">
                                        View and manage your account information
                                    </p>
                                </div>
                                <div
                                    className={`px-4 py-2 rounded-full text-sm font-semibold ${user?.role === "MANAGER"
                                        ? "bg-purple-100 text-purple-700"
                                        : "bg-blue-100 text-blue-700"
                                        }`}
                                >
                                    {user?.role}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Account Information */}
                    <div className="bg-white shadow rounded-lg mb-6">
                        <div className="px-6 py-6 sm:px-8">
                            <h2 className="text-xl font-semibold text-gray-900 mb-4">
                                Account Information
                            </h2>
                            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                                <div>
                                    <label className="block text-sm font-medium text-gray-500">
                                        Phone Number
                                    </label>
                                    <p className="mt-1 text-lg text-gray-900">{user?.phone}</p>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-500">
                                        User ID
                                    </label>
                                    <p className="mt-1 text-lg text-gray-900">{user?.id}</p>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-500">
                                        Account Role
                                    </label>
                                    <p className="mt-1 text-lg text-gray-900">{user?.role}</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Loyalty Points */}
                    <div className="bg-white shadow rounded-lg mb-6">
                        <div className="px-6 py-6 sm:px-8">
                            <h2 className="text-xl font-semibold text-gray-900 mb-4">
                                Loyalty Points
                            </h2>
                            <div className="bg-gradient-to-r from-indigo-500 to-purple-600 rounded-lg p-6 text-white">
                                <div className="flex items-center justify-between">
                                    <div>
                                        <p className="text-sm opacity-90">Available Points</p>
                                        <p className="text-4xl font-bold mt-1">0</p>
                                        <p className="text-xs opacity-75 mt-2">
                                            Coming soon - Earn points with every purchase!
                                        </p>
                                    </div>
                                    <div>
                                        <svg
                                            className="w-16 h-16 opacity-75"
                                            fill="none"
                                            stroke="currentColor"
                                            viewBox="0 0 24 24"
                                        >
                                            <path
                                                strokeLinecap="round"
                                                strokeLinejoin="round"
                                                strokeWidth={2}
                                                d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                                            />
                                        </svg>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Actions */}
                    <div className="bg-white shadow rounded-lg">
                        <div className="px-6 py-6 sm:px-8">
                            <h2 className="text-xl font-semibold text-gray-900 mb-4">
                                Account Actions
                            </h2>
                            <div className="space-y-3">
                                <button
                                    disabled
                                    className="w-full text-left px-4 py-3 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    <div className="flex justify-between items-center">
                                        <span className="text-gray-700">Edit Profile</span>
                                        <span className="text-xs text-gray-500">Coming Soon</span>
                                    </div>
                                </button>
                                <button
                                    disabled
                                    className="w-full text-left px-4 py-3 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    <div className="flex justify-between items-center">
                                        <span className="text-gray-700">Order History</span>
                                        <span className="text-xs text-gray-500">Coming Soon</span>
                                    </div>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </ProtectedRoute>
    );
}
