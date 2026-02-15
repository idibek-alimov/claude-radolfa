# User Management Alignment Audit & Recommendations

## Overview
This report analyzes the current state of user management in the Radolfa project, focusing on the alignment between backend entities and frontend profile/management features.

## Backend Analysis: Current State
The backend supports basic profile management but lacks several social and administrative features common in modern e-commerce applications.

### 1. Data Model (`UserEntity`)
*   **Attributes**: `id`, `phone` (unique identifier), `role`, `name`, `email`, `loyalty_points`.
*   **Infrastructure**: Extends `BaseAuditEntity` for timestamps and optimistic locking.
*   **Security**: Integrated with JWT-based authentication.

### 2. API Capabilities (`UserController`)
*   **Read**: User info is typically returned during login or via `/api/v1/auth/me` (handled in `AuthController`).
*   **Update**: `PUT /api/v1/users/profile` allows updating `name` and `email`.

### 3. Missing Infrastructure
*   **Avatar Storage**: No field for `avatar_url` or logic for profile picture management.
*   **Account Status**: No `enabled` or `blocked` flags for administrative control.
*   **Activity Tracking**: No `last_login_at` or registration date fields.

---

## Frontend Analysis: Current State
The frontend implements basic profile viewing and editing within the `/profile` route.

### 1. Profile Page (`/profile/page.tsx`)
*   **State Management**: Uses `@tanstack/react-query` for fetching and updating user data.
*   **Functionality**: Users can view their phone number and loyalty points, and edit their name and email.
*   **Validation**: Basic client-side validation for email formats.

---

## Recommendations for Improvement

### Phase 1: User Profile Enrichment
| Feature | Backend Task | Frontend Task |
| :--- | :--- | :--- |
| **Avatars** | Add `avatar_url` to entity/DTOs | Implement image upload & display |
| **Birth Date** | Add `birth_date` for marketing | Add date picker to profile form |
| **Loyalty UI** | N/A (Existing data) | Add "Tier" visualization (Silver/Gold) |

### Phase 2: Manager Dashboard (User Management)
Currently, managers have no way to view or manage users from the `/manage` page.

1.  **User Directory**:
    *   **Backend**: Implement `GET /api/v1/users` (Paginated) with search by phone/name.
    *   **Frontend**: Add a "Users" tab to the admin dashboard with a searchable table.
2.  **Administrative Actions**:
    *   **Backend**: Add `PATCH /api/v1/users/{id}/status` to block/unblock users.
    *   **Frontend**: Add "Block User" button to the management list.
3.  **Role Management**:
    *   **Backend**: Add endpoint to promote users to `MANAGER` (restricted to `SYSTEM` or super-admin).

### Phase 3: Enhanced UX
1.  **Address Book**:
    *   Create a separate `AddressEntity` linked to users to allow multiple shipping addresses.
2.  **Notification Preferences**:
    *   Add flags for SMS/Email notifications for orders.

---

## Technical Debt to Address
*   **Email Uniqueness**: The backend `UserEntity` marks email as unique, but the `updateProfile` method needs robust error handling for `DataIntegrityViolationException` when a user tries to change to an email already taken.
*   **Input Sanitization**: Ensure name and email are trimmed and sanitized before being saved to the database.
