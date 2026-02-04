// AUTO-GENERATED FROM BACKEND - DO NOT EDIT
//
// Sources:
//   tj.radolfa.domain.model.Product                  (class)
//   tj.radolfa.infrastructure.web.dto.ProductImageResponseDto  (record)
//
// Re-run `/bridge` after any change to the Java source.
//
// ── Type-mapping key ─────────────────────────────────────────────
//   Java Long        → number | null     (boxed; null on unsaved entities)
//   Java String      → string | string | null  (see per-field note)
//   Java BigDecimal  → number | null     (nullable before first ERP sync)
//   Java Integer     → number | null     (boxed; nullable before first ERP sync)
//   Java boolean     → boolean           (primitive; never null)
//   Java List<T>     → T[]               (always initialised; may be empty)
//   Java Instant     → string | null     (ISO-8601 over the wire)
// ─────────────────────────────────────────────────────────────────

/**
 * Full product aggregate as returned by the backend.
 *
 * Field-ownership contract (enforced server-side; mirrored here for
 * the UI to honour):
 *
 *   LOCKED   – set exclusively by ERP sync.  The frontend must never
 *              render an <input> or any editable control for these fields.
 *   EDITABLE – owned by Radolfa.  Managers may update via the API.
 *   AUDIT    – read-only timestamps stamped by the backend.
 */
export interface Product {
  /** Internal DB primary key.  Opaque to the frontend — prefer {@link erpId}. */
  id: number | null;

  /** Stable identifier assigned by ERPNext.  Use as the React key in lists. */
  erpId: string;

  /** Product name.                                LOCKED – ERP source of truth. */
  name: string | null;

  /** Unit price in the shop currency (USD).       LOCKED – ERP source of truth. */
  price: number | null;

  /** Current stock count.                         LOCKED – ERP source of truth. */
  stock: number | null;

  /** Marketing description written by the team.   EDITABLE – Radolfa owned. */
  webDescription: string | null;

  /** Curated "top seller" flag.                   EDITABLE – Radolfa owned. */
  topSelling: boolean;

  /** S3 image URLs produced by the image pipeline. EDITABLE – Radolfa owned. */
  images: string[];

  /** ISO-8601 timestamp of the most recent ERP sync. AUDIT – read-only. */
  lastErpSyncAt: string | null;
}

// ── Response DTOs ────────────────────────────────────────────────
// These mirror the records in tj.radolfa.infrastructure.web.dto.
// They are placed here (product slice) because they are exclusively
// returned by product-scoped endpoints.

/**
 * Response body of `POST /api/v1/products/{erpId}/images`.
 *
 * Source: tj.radolfa.infrastructure.web.dto.ProductImageResponseDto
 *   String        erpId   → string
 *   List<String>  images  → string[]
 */
export interface ProductImageResponse {
  /** The ERP identifier of the product that was updated. */
  erpId: string;
  /** Full image-URL list after the new upload was appended. */
  images: string[];
}
