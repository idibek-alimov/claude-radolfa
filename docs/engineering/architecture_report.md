# Architectural Report: Shared vs. Separated Frontends

## Current System Overview

After analyzing both the frontend and backend codebases, here is the current architectural state:

### Backend (Security & Roles)
- **Role-Based Access Control (RBAC)**: The backend (Spring Security) strictly enforces roles.
- **MANAGER Role**: Specifically permitted to upload images and edit product descriptions.
- **SYSTEM Role**: Handles critical ERP synchronization (prices, names, stock).
- **Public access**: Only `GET` requests for products and auth endpoints are public.

### Frontend (User Interface)
- **Next.js Route Groups**: The app already uses `(admin)` and `(storefront)` route groups to logically isolate manager features from user features.
- **ProtectedRoute**: A higher-order component handles client-side role verification before rendering restricted pages.
- **Shared Components**: Common UI elements (buttons, dialogs, tables) are shared from a `shared/ui` directory, ensuring design consistency.

---

## Comparison of Approaches

| Feature | Single App (Current) | Two Separate Apps |
| :--- | :--- | :--- |
| **Security** | High (Backend-enforced) | High (Backend-enforced) |
| **Complexity** | Low (Single repo, shared types) | High (Dual repos or Monorepo needed) |
| **Maintenance** | Easy (Sync changes across roles) | Harder (Duplicate code or shared library) |
| **Performance** | Good (Next.js code splitting) | Optimal (Smaller initial bundle for users) |
| **Development** | Faster (Shared UI and API logic) | Slower (Managing shared dependencies) |

---

## Core Considerations

### 1. Security Logic
Separating the frontend provides a "physical" barrier, but doesn't inherently increase security. True security lives in the **Backend**. Since your backend already validates the `MANAGER` role for sensitive operations (PUT/POST), a regular user cannot perform manager actions even if they discovered the admin URL.

### 2. Performance (Bundle Size)
Next.js naturally handles this via **Code Splitting**. Logic inside the `(admin)` folder is only loaded when someone visits an admin route (`/manage`). Regular users browsing the storefront do not download the heavy manager-specific code.

### 3. Developer Productivity
Currently, you share TypeScript interfaces (e.g., `Product`) and API call logic between both roles. Moving to two apps would require:
- A Monorepo setup (Turborepo/Nx) to share code.
- Duplicating many components or creating a private UI library.

### 4. User Experience (UX)
If a person is both a customer and a manager, they can stay within one application and one authentication session. Two apps would likely require separate logins or a complex Single Sign-On (SSO) configuration.

---

## Recommendation: STICK WITH A SINGLE APP

You should **not** separate the frontends at this stage. Your current architecture is clean, professional, and already handles role separation effectively through Next.js Route Groups.

### Why?
1. **Logical Separation exists**: The `(admin)` and `(storefront)` folders already provide the organizational benefits of two apps without the deployment overhead.
2. **Maintenance is streamlined**: You can update a product's data shape in one place and have it reflect in both the customer view and the manager dashboard.
3. **No performance penalty**: Next.js ensures that ordinary users only download storefront code.
4. **Backend is the gatekeeper**: Your security is correctly implemented at the API layer, which is the only place where it truly matters.

### Suggested Next Steps (Keep It Shared)
- **Strict Layouts**: Ensure `(admin)` has a completely different `layout.tsx` from `(storefront)` to give managers a focused "Dashboard" feel.
- **Admin-only assets**: Keep heavy admin-only libraries (like complex charts or large forms) localized to the `(admin)` folder to maintain storefront speed.
