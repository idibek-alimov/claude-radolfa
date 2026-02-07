# Roadmap Phase 1: Stabilize (Security & State Management)

### PERSONA
You are a **Senior Fullstack Engineer (FAANG)**. You are tasked with upgrading the authentication and data-fetching architecture to meet industry-best standards for security and performance.

### TASK
1.  **Shift Authentication from LocalStorage to HTTP-Only Cookies**.
2.  **Migrate imperative fetching (Axios/useEffect) to TanStack Query (React Query)**.

### SCOPE & TARGETS
- **Backend Auth**: `backend/src/main/java/tj/radolfa/infrastructure/web/SecurityConfig.java`, `JwtAuthenticationFilter.java`, and the Login controller.
- **Frontend Core**: `frontend/src/shared/api/axios.ts`, `frontend/src/features/auth/model/useAuth.ts`.
- **Frontend Pages**: Refactor `frontend/src/app/(admin)/manage/page.tsx` to use Query hooks.

### REQUIREMENTS

#### 1. Security: Secure Cookie Auth
- **Backend**:
  - Update `JwtAuthenticationFilter` to extract the JWT from an `auth_token` cookie instead of the `Authorization` header.
  - Update the Auth Controller to set an **HTTP-Only, Secure, SameSite=Strict** cookie upon successful login.
  - Implement a `/api/v1/auth/logout` endpoint that clears the cookie.
- **Frontend**:
  - Remove logic in `axios.ts` and `useAuth.ts` that reads from `localStorage`.
  - Ensure `apiClient` in `axios.ts` is configured with `withCredentials: true`.

#### 2. Performance: TanStack Query Migration
- **Infrastructure**: Install `@tanstack/react-query` and configure a `QueryClientProvider` in the root layout.
- **Data Hook**: Create a `useProducts` hook in `frontend/src/features/products/model/queries.ts`:
  - Fetching: Re-use the existing API functions.
  - State: Provide `isLoading`, `isError`, and the `products` list.
- **Refactor**: Update the Product Management page to use `useProducts`. Remove local `products` state, `loading` state, and the `useEffect` fetcher.

### SUCCESS CRITERIA
- No JWT tokens are visible in the "Application -> Local Storage" tab of the browser.
- The Admin dashboard loads with background revalidation (no unnecessary spinners on mount).
- Standard login/logout flows are perfectly intact but secured via Cookies.
---
