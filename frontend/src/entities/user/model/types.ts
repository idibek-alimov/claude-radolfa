// AUTO-GENERATED FROM BACKEND - DO NOT EDIT
//
// Sources:
//   tj.radolfa.domain.model.UserRole  (enum)
//   tj.radolfa.domain.model.User      (record)
//
// Re-run `/bridge` after any change to the Java source.
//
// ── Type-mapping key ─────────────────────────────────────────────
//   Java enum    → TypeScript string-enum  (@Enumerated(STRING) on the wire)
//   Java Long    → number | null           (boxed)
//   Java String  → string                  (phone is non-null by DB constraint)
// ─────────────────────────────────────────────────────────────────

/**
 * Authorised roles.  Serialised as plain strings over JSON because the
 * JPA entity uses {@code @Enumerated(EnumType.STRING)}.
 *
 * Source: tj.radolfa.domain.model.UserRole
 *
 * Security note:
 *   SYSTEM – the only role permitted to write ERP-locked fields.
 *            The frontend should never expose SYSTEM-role actions.
 */
export enum UserRole {
  USER = "USER",
  MANAGER = "MANAGER",
  SYSTEM = "SYSTEM",
}

/**
 * Immutable user shape as returned by the backend.
 *
 * Source: tj.radolfa.domain.model.User
 *   Long      id    → number | null
 *   String    phone → string
 *   UserRole  role  → UserRole
 */
export interface User {
  /** Internal DB primary key. */
  id: number | null;
  /** Phone number — the unique identifier used for OTP login (Phase 5). */
  phone: string;
  /** The authorised role granted to this user. */
  role: UserRole;
  /** Display name (optional, set via profile). */
  name?: string;
  /** Email (optional, set via profile). */
  email?: string;
  /** Loyalty points synced from ERPNext. */
  loyaltyPoints: number;
  /** Whether the user account is active. */
  enabled: boolean;
}
