/**
 * Format a price value for display.
 * Returns "—" for null/undefined values.
 */
export function formatPrice(price: number | null | undefined): string {
  if (price == null) return "—";
  return `$${price.toFixed(2)}`;
}

/**
 * Format an ISO date string for display.
 * Returns "—" for null/undefined values.
 */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString();
}
