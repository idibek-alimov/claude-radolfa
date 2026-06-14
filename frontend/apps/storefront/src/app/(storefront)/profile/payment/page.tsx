"use client";

import { CreditCard, Plus } from "lucide-react";
import { useTranslations } from "next-intl";

/** Dashed empty state — design-system "coming soon" / no-data block, card icon variant. */
function PaymentEmptyState({ message }: { message: string }) {
  return (
    <div className="border border-dashed border-ink/15 rounded-xl p-12 flex flex-col items-center text-center gap-3">
      <CreditCard className="h-10 w-10 text-ink/20" />
      <p className="text-sm text-ink/55">{message}</p>
    </div>
  );
}

// Phase 11 — Payment section structure with honest empty states. Saved cards
// and wallet balance are coming-soon; never fabricate mockup sample data.
export default function ProfilePaymentPage() {
  const t = useTranslations("profile");

  return (
    <section className="space-y-6">
      <div className="flex items-end justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-[28px] font-black leading-tight">{t("paymentMethodsTitle")}</h1>
          <p className="text-ink/55 text-[13px] mt-1">{t("paymentSubtitle")}</p>
        </div>
        <button
          type="button"
          disabled
          aria-disabled="true"
          title={t("comingSoon")}
          className="h-10 px-5 rounded-full bg-mag text-white font-bold text-[13px] inline-flex items-center gap-2 opacity-50 cursor-not-allowed"
        >
          <Plus className="h-4 w-4" />
          {t("addCard")}
        </button>
      </div>

      <div>
        <h2 className="font-black text-lg mb-3">{t("statWallet")}</h2>
        <PaymentEmptyState message={t("walletComingSoon")} />
      </div>

      <div>
        <h2 className="font-black text-lg mb-3">{t("savedCards")}</h2>
        <PaymentEmptyState message={t("paymentComingSoon")} />
      </div>
    </section>
  );
}
