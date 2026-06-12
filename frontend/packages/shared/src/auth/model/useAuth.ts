"use client";

import { useState, useEffect, useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { User } from "./types";
import { currentAppLoginPath, currentAppHomePath } from "../../lib/appNav";

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

interface UseAuthReturn extends AuthState {
  logout: () => void;
  updateUser: (user: User) => void;
  /** Re-fetch /auth/me to refresh loyalty data (call after purchase, etc.) */
  refreshUser: () => void;
}

/**
 * Custom hook for managing authentication state via HTTP-only cookies.
 *
 * On mount, calls GET /api/v1/auth/me to resolve the current user
 * from the cookie-authenticated session. No tokens are stored client-side.
 */
export function useAuth(): UseAuthReturn {
  const queryClient = useQueryClient();
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
  });

  useEffect(() => {
    let cancelled = false;

    async function fetchCurrentUser() {
      try {
        const { data } = await apiClient.get<User>("/api/v1/users/me");
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
    queryClient.clear();
    // Same-app logout: ops staff land on /ops/login, storefront customers on /.
    // Full reload (window.location) bypasses the basePath-aware Next.js router
    // so the path is not double-prefixed.
    window.location.assign(
      window.location.pathname.startsWith("/ops")
        ? currentAppLoginPath()
        : currentAppHomePath(),
    );
  }, [queryClient]);

  const updateUser = useCallback((newUser: User) => {
    setAuthState((prev) => ({ ...prev, user: newUser }));
  }, []);

  const refreshUser = useCallback(async () => {
    try {
      const { data } = await apiClient.get<User>("/api/v1/users/me");
      setAuthState({ user: data, isAuthenticated: true, isLoading: false });
    } catch {
      // If refresh fails, keep current state
    }
  }, []);

  return {
    ...authState,
    logout,
    updateUser,
    refreshUser,
  };
}
