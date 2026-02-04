// AUTO-GENERATED FROM BACKEND - DO NOT EDIT
//
// Source: tj.radolfa.infrastructure.web.dto.SyncResultDto  (record)
// Re-run `/bridge` after any change to the Java source.
//
// ── Type-mapping key ─────────────────────────────────────────────
//   Java int  → number   (primitive; never null)
// ─────────────────────────────────────────────────────────────────

/**
 * Response body of `POST /api/v1/sync/products`.
 *
 * This is a system-level DTO consumed only by internal tooling or
 * admin dashboards — not by the public storefront.  Placed in
 * {@code shared/api} because it is not scoped to a single entity slice.
 *
 * Source: tj.radolfa.infrastructure.web.dto.SyncResultDto
 *   int  synced → number   – count of products successfully upserted
 *   int  errors → number   – count of snapshots that failed (logged, non-fatal)
 */
export interface SyncResult {
  synced: number;
  errors: number;
}
