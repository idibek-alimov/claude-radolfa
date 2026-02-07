import axios from "axios";

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

export default apiClient;
