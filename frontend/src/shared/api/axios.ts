import axios from "axios";

/**
 * Single Axios instance shared across the entire app.
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
});

// Add auth token to requests if available
apiClient.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

export default apiClient;
