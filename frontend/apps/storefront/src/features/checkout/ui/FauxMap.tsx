"use client";

import { useTranslations } from "next-intl";

/**
 * Decorative, non-functional map for the Delivery step's Courier panel.
 * No map library, no geocoding — the address stays the existing free-text
 * field. "Use my location" / "Locate me" intentionally has no handler.
 */
export function FauxMap() {
  const t = useTranslations("checkout.delivery.home");

  return (
    <div className="relative h-44 md:h-52 rounded-xl overflow-hidden border border-ink/10 map-bg mb-3">
      <div
        className="absolute bg-white h-[8px] md:h-[9px]"
        style={{ top: "38%", left: "-4%", width: "108%", transform: "rotate(-4deg)" }}
      />
      <div
        className="absolute bg-white w-[8px] md:w-[9px]"
        style={{ top: 0, left: "64%", height: "100%", transform: "rotate(6deg)" }}
      />
      <div
        className="absolute bg-white/70 h-[5px] md:h-[6px]"
        style={{ top: "70%", left: "-4%", width: "108%", transform: "rotate(2deg)" }}
      />

      {/* centered pin */}
      <div className="absolute left-1/2 top-1/2" style={{ transform: "translate(-50%,-100%)" }}>
        <svg
          className="w-[34px] h-[34px] md:w-9 md:h-9"
          viewBox="0 0 24 24"
          fill="#CB11AB"
          stroke="white"
          strokeWidth="1.5"
          style={{ filter: "drop-shadow(0 3px 5px rgba(26,10,24,0.3))" }}
        >
          <path d="M20 10c0 6-8 11-8 11s-8-5-8-11a8 8 0 0 1 16 0z" />
          <circle cx="12" cy="10" r="2.6" fill="white" stroke="none" />
        </svg>
      </div>
      <span
        className="absolute left-1/2 top-1/2 w-2 h-2 md:w-2.5 md:h-2.5 rounded-full bg-ink/25"
        style={{ transform: "translate(-50%,-50%)" }}
      />

      <button
        type="button"
        className="absolute bottom-2.5 right-2.5 md:bottom-3 md:right-3 h-8 md:h-9 px-3 md:px-3.5 rounded-full bg-white shadow-md text-[11px] md:text-[12px] font-bold text-ink inline-flex items-center gap-1.5 md:hover:bg-soft"
      >
        <svg
          width="13"
          height="13"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#CB11AB"
          strokeWidth="2.2"
          className="md:hidden"
        >
          <circle cx="12" cy="12" r="3" />
          <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
        </svg>
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#CB11AB"
          strokeWidth="2.2"
          className="hidden md:block"
        >
          <circle cx="12" cy="12" r="3" />
          <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
        </svg>
        <span className="md:hidden">{t("locateMe")}</span>
        <span className="hidden md:inline">{t("useMyLocation")}</span>
      </button>

      <div className="hidden md:block absolute bottom-3 left-3 px-2.5 py-1.5 rounded-lg bg-ink/70 text-white text-[11px] font-mono tracking-tight">
        {t("dragPinHint")}
      </div>
    </div>
  );
}
