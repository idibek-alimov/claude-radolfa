const priceFormatter = new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 });

export function formatPrice(price: number | null | undefined): string {
  if (price == null) return "—";
  // narrow no-break space as thousands separator → "3 807 TJS"
  return `${priceFormatter.format(price).replace(/,/g, " ")} TJS`;
}

/**
 * Format a price range for display.
 * If start and end are the same, shows a single price.
 * If different, shows "from – to".
 * Returns "—" if both are null.
 */
export function formatPriceRange(
  start: number | null | undefined,
  end: number | null | undefined
): string {
  if (start == null && end == null) return "—";
  if (start == null) return formatPrice(end);
  if (end == null) return formatPrice(start);
  if (start === end) return formatPrice(start);
  return `${formatPrice(start)} – ${formatPrice(end)}`;
}

/**
 * Format an ISO date string for display.
 * Returns "—" for null/undefined values.
 */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString();
}
