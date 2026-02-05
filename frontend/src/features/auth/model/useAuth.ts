"use client";

import { useState, useEffect } from "react";
import type { User } from "./types";

interface AuthState {
    user: User | null;
    token: string | null;
    isAuthenticated: boolean;
    isLoading: boolean;
}

interface UseAuthReturn extends AuthState {
    logout: () => void;
    updateUser: (user: User) => void;
}

/**
 * Custom hook for managing authentication state.
 * 
 * Reads token and user from localStorage on mount and provides:
 * - Current user info (phone, role)
 * - Authentication status
 * - Logout function
 * 
 * @example
 * ```tsx
 * const { user, isAuthenticated, logout } = useAuth();
 * 
 * if (!isAuthenticated) {
 *   return <Link href="/login">Login</Link>;
 * }
 * 
 * return (
 *   <div>
 *     <p>Welcome, {user.phone}</p>
 *     <button onClick={logout}>Logout</button>
 *   </div>
 * );
 * ```
 */
export function useAuth(): UseAuthReturn {
    const [authState, setAuthState] = useState<AuthState>({
        user: null,
        token: null,
        isAuthenticated: false,
        isLoading: true,
    });

    useEffect(() => {
        // Only run on client side
        if (typeof window === "undefined") {
            return;
        }

        try {
            const token = localStorage.getItem("token");
            const userJson = localStorage.getItem("user");

            if (token && userJson) {
                const user = JSON.parse(userJson) as User;
                setAuthState({
                    user,
                    token,
                    isAuthenticated: true,
                    isLoading: false,
                });
            } else {
                setAuthState({
                    user: null,
                    token: null,
                    isAuthenticated: false,
                    isLoading: false,
                });
            }
        } catch (error) {
            console.error("Failed to parse auth data from localStorage:", error);
            setAuthState({
                user: null,
                token: null,
                isAuthenticated: false,
                isLoading: false,
            });
        }
    }, []);

    const logout = () => {
        // Clear localStorage
        localStorage.removeItem("token");
        localStorage.removeItem("user");

        // Update state
        setAuthState({
            user: null,
            token: null,
            isAuthenticated: false,
            isLoading: false,
        });

        // Redirect to home
        window.location.href = "/";
    };

    const updateUser = (newUser: User) => {
        localStorage.setItem("user", JSON.stringify(newUser));
        setAuthState(prev => ({ ...prev, user: newUser }));
    };

    return {
        ...authState,
        logout,
        updateUser,
    };
}
