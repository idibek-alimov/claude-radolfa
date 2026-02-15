import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";

/**
 * Single Axios instance shared across the entire app.
 *
 * Authentication is handled via HTTP-only cookies (set by the backend).
 * `withCredentials: true` ensures cookies are sent on every request.
 *
 * In production, the frontend is behind nginx which proxies /api/* to the backend.
 * So we use an empty baseURL to make relative requests.
 *
 * In development (outside Docker), you can set NEXT_PUBLIC_API_BASE_URL
 * to point directly to the backend (e.g., http://localhost:8080).
 */
const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
});

let isRefreshing = false;
let failedQueue: {
  resolve: (value: unknown) => void;
  reject: (reason: unknown) => void;
  config: InternalAxiosRequestConfig;
}[] = [];

function processQueue(error: AxiosError | null) {
  failedQueue.forEach(({ resolve, reject, config }) => {
    if (error) {
      reject(error);
    } else {
      resolve(apiClient(config));
    }
  });
  failedQueue = [];
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config;

    // Only attempt refresh on 401, and not for auth endpoints themselves
    if (
      error.response?.status !== 401 ||
      !originalRequest ||
      originalRequest.url?.includes("/api/v1/auth/refresh") ||
      originalRequest.url?.includes("/api/v1/auth/login") ||
      originalRequest.url?.includes("/api/v1/auth/verify")
    ) {
      return Promise.reject(error);
    }

    // If already refreshing, queue this request
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject, config: originalRequest });
      });
    }

    isRefreshing = true;

    try {
      await axios.post(
        `${API_BASE}/api/v1/auth/refresh`,
        {},
        { withCredentials: true }
      );

      processQueue(null);

      // Retry the original request with new cookies
      return apiClient(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError as AxiosError);

      // Refresh failed — redirect to login
      if (
        typeof window !== "undefined" &&
        !window.location.pathname.startsWith("/login")
      ) {
        window.location.href = "/login";
      }

      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default apiClient;
