"use client";

import { useState, useEffect, useCallback } from "react";
import apiClient from "@/shared/api/axios";
import type { User } from "./types";

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

interface UseAuthReturn extends AuthState {
  logout: () => void;
  updateUser: (user: User) => void;
}

/**
 * Custom hook for managing authentication state via HTTP-only cookies.
 *
 * On mount, calls GET /api/v1/auth/me to resolve the current user
 * from the cookie-authenticated session. No tokens are stored client-side.
 */
export function useAuth(): UseAuthReturn {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
  });

  useEffect(() => {
    let cancelled = false;

    async function fetchCurrentUser() {
      try {
        const { data } = await apiClient.get<User>("/api/v1/auth/me");
        if (!cancelled) {
          setAuthState({
            user: data,
            isAuthenticated: true,
            isLoading: false,
          });
        }
      } catch {
        if (!cancelled) {
          setAuthState({
            user: null,
            isAuthenticated: false,
            isLoading: false,
          });
        }
      }
    }

    fetchCurrentUser();
    return () => { cancelled = true; };
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiClient.post("/api/v1/auth/logout");
    } catch {
      // Cookie may already be expired — ignore
    }
    setAuthState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
    window.location.href = "/";
  }, []);

  const updateUser = useCallback((newUser: User) => {
    setAuthState((prev) => ({ ...prev, user: newUser }));
  }, []);

  return {
    ...authState,
    logout,
    updateUser,
  };
}
