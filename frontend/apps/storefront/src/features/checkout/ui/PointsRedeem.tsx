"use client";

import { useTranslations } from "next-intl";
import { formatPrice } from "@radolfa/shared/lib/format";

interface PointsRedeemProps {
  availablePoints: number;
  pointsToRedeem: number;
  setPointsToRedeem: (value: number) => void;
}

export function PointsRedeem({
  availablePoints,
  pointsToRedeem,
  setPointsToRedeem,
}: PointsRedeemProps) {
  const t = useTranslations("checkout");

  if (availablePoints <= 0) return null;

  return (
    <div className="border-t border-ink/8 mt-4 pt-4">
      <div className="text-[13px] font-bold">{t("loyaltyPoints")}</div>
      <p className="text-[12px] text-ink/55 mt-0.5 mb-2.5">
        {t("availablePoints", { count: availablePoints })}
      </p>
      <div className="flex items-center gap-3">
        <input
          type="number"
          min={0}
          max={availablePoints}
          value={pointsToRedeem === 0 ? "" : pointsToRedeem}
          onChange={(e) => {
            const val = Math.min(
              Math.max(0, parseInt(e.target.value, 10) || 0),
              availablePoints
            );
            setPointsToRedeem(val);
          }}
          placeholder="0"
          className="field w-28 md:w-32"
        />
        <span className="text-[13px] text-ink/55 tabular-nums">
          {pointsToRedeem > 0 && `= ${formatPrice(pointsToRedeem * 0.01)}`}
        </span>
      </div>
    </div>
  );
}
