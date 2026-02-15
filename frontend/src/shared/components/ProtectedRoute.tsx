"use client";

import { useAuth } from "@/features/auth";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

interface ProtectedRouteProps {
    children: React.ReactNode;
    requiredRole?: "USER" | "MANAGER" | "SYSTEM";
}

const ROLE_HIERARCHY: Record<string, number> = {
    USER: 0,
    MANAGER: 1,
    SYSTEM: 2,
};

function hasRequiredRole(
    userRole: string | undefined,
    requiredRole: string | undefined
): boolean {
    if (!requiredRole) return true;
    if (!userRole) return false;
    return (ROLE_HIERARCHY[userRole] ?? 0) >= (ROLE_HIERARCHY[requiredRole] ?? 0);
}

export default function ProtectedRoute({
    children,
    requiredRole,
}: ProtectedRouteProps) {
    const { user, isAuthenticated, isLoading } = useAuth();
    const router = useRouter();

    useEffect(() => {
        if (!isLoading) {
            // Not authenticated at all
            if (!isAuthenticated) {
                router.push("/login");
                return;
            }

            // Check role if required
            if (!hasRequiredRole(user?.role, requiredRole)) {
                router.push("/");
            }
        }
    }, [isAuthenticated, isLoading, user, requiredRole, router]);

    // Show loading state
    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                    <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
                    <p className="mt-2 text-gray-600">Loading...</p>
                </div>
            </div>
        );
    }

    // Not authenticated
    if (!isAuthenticated) {
        return null;
    }

    // Wrong role
    if (!hasRequiredRole(user?.role, requiredRole)) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <div className="max-w-md w-full bg-white shadow-lg rounded-lg p-8 text-center">
                    <div className="mb-4">
                        <svg
                            className="mx-auto h-12 w-12 text-red-500"
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
                    <h2 className="text-2xl font-bold text-gray-900 mb-2">
                        Access Denied
                    </h2>
                    <p className="text-gray-600 mb-6">
                        You don't have permission to access this page. This page requires{" "}
                        <span className="font-semibold">{requiredRole}</span> role.
                    </p>
                    <button
                        onClick={() => router.push("/")}
                        className="w-full bg-indigo-600 text-white py-2 px-4 rounded-md hover:bg-indigo-700 transition-colors"
                    >
                        Go to Home
                    </button>
                </div>
            </div>
        );
    }

    return <>{children}</>;
}
