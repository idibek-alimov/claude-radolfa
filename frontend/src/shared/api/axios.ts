import axios from "axios";

/**
 * Single Axios instance shared across the entire app.
 * Base URL is driven by an environment variable so that
 * local dev, staging, and production all point at the right backend
 * without a code change.
 */
const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    "Content-Type": "application/json",
  },
});

export default apiClient;
