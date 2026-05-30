import type { DiscountResponse } from "../model/types";

export function DiscountStatusBadge({ discount }: { discount: DiscountResponse }) {
  return (
    <span
      className="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold text-white"
      style={{ backgroundColor: `#${discount.colorHex}` }}
    >
      {discount.title} −{discount.amountValue}{discount.amountType === "FIXED" ? " TJS" : "%"}
    </span>
  );
}
