"use client";

import Link from "next/link";
import { Sparkles } from "lucide-react";
import { useTranslations } from "next-intl";
import { useAuth } from "@radolfa/shared/auth";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { useMyOrders, useOrderSummary } from "@/features/profile/api";
import { OverviewOrderRow } from "@/features/profile/ui/OverviewOrderRow";
import { useMyAddresses } from "@/entities/address";
import type { AddressLabel } from "@/entities/address";
import { ProfileLoyaltyHero } from "@/entities/loyalty";

const ADDRESS_LABEL_KEYS: Record<AddressLabel, string> = {
  HOME: "labelHome",
  WORK: "labelWork",
  OTHER: "labelOther",
};

function formatPoints(points: number): string {
  return points.toLocaleString("en-US").replace(/,/g, " ");
}

/** Dashed empty state — design-system "coming soon" / no-data block. */
function EmptyBlock({ message }: { message: string }) {
  return (
    <div className="border border-dashed border-ink/15 rounded-xl p-12 flex flex-col items-center text-center gap-3">
      <Sparkles className="h-10 w-10 text-ink/20" />
      <p className="text-sm text-ink/55">{message}</p>
    </div>
  );
}

// Phase 7 — the real Overview dashboard. Loyalty + recent orders + addresses
// preview are real; vouchers, wallet and payment previews render the
// design-system coming-soon empty state (no fabricated data).
export default function ProfileOverviewPage() {
  const t = useTranslations("profile");
  const { user } = useAuth();
  const firstName = user?.name?.split(" ")[0] || user?.phone || "";

  const { data: ordersPage, isLoading: ordersLoading } = useMyOrders(1, 3);
  const { data: summary } = useOrderSummary();
  const { data: addresses, isLoading: addressesLoading } = useMyAddresses();

  const totalOrders = ordersPage?.totalElements ?? 0;
  const inProgress = summary?.progress ?? 0;
  const loyalty = user?.loyalty ?? null;

  return (
    <section className="space-y-6">
      <h1 className="text-[30px] font-black leading-tight">
        {t("greeting", { name: firstName })}
      </h1>

      <ProfileLoyaltyHero loyalty={loyalty} />

      {/* Stat tiles */}
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
        <Link
          href="/profile/orders"
          className="block text-left rounded-2xl lg:rounded-3xl bg-soft p-3 lg:p-5 hover:ring-2 hover:ring-mag/30 transition"
        >
          <div className="text-[11px] lg:text-[12px] text-ink/55">{t("statTotalOrders")}</div>
          <div className="text-[24px] lg:text-[30px] font-black text-mag tabular-nums leading-tight">
            {totalOrders}
          </div>
          {inProgress > 0 && (
            <div className="text-[10px] lg:text-[11px] text-emerald font-semibold">
              {t("statInProgress")} · {inProgress}
            </div>
          )}
        </Link>

        <div className="rounded-2xl lg:rounded-3xl bg-soft p-3 lg:p-5">
          <div className="text-[11px] lg:text-[12px] text-ink/55">{t("statVouchers")}</div>
          <div className="text-[24px] lg:text-[30px] font-black text-mag tabular-nums leading-tight">—</div>
          <div className="text-[10px] lg:text-[11px] text-ink/55">{t("noVouchersYet")}</div>
        </div>

        <Link
          href="/profile/payment"
          className="block text-left rounded-2xl lg:rounded-3xl bg-soft p-3 lg:p-5 hover:ring-2 hover:ring-mag/30 transition"
        >
          <div className="text-[11px] lg:text-[12px] text-ink/55">{t("statWallet")}</div>
          <div className="text-[24px] lg:text-[30px] font-black text-mag tabular-nums leading-tight">—</div>
          <div className="text-[10px] lg:text-[11px] text-ink/55">{t("paymentComingSoon")}</div>
        </Link>

        <Link href="/profile/rewards" className="lg:hidden block text-left rounded-2xl bg-soft p-3">
          <div className="text-[11px] text-ink/55">{t("statPoints")}</div>
          <div className="text-[24px] font-black text-mag tabular-nums leading-tight">
            {formatPoints(loyalty?.points ?? 0)}
          </div>
          {loyalty?.tier && <div className="text-[10px] text-ink/55">{loyalty.tier.name}</div>}
        </Link>
      </div>

      <div className="grid grid-cols-12 gap-6">
        {/* Recent orders */}
        <section className="col-span-12 xl:col-span-7 rounded-3xl bg-white border border-ink/8 p-4 lg:p-6">
          <div className="flex items-baseline justify-between mb-4">
            <h2 className="font-black text-lg lg:text-xl">{t("recentOrders")}</h2>
            <Link
              href="/profile/orders"
              className="text-mag text-[12px] lg:text-[13px] font-bold hover:underline"
            >
              {t("viewAllOrders", { count: totalOrders })}
            </Link>
          </div>

          {ordersLoading ? (
            <div className="space-y-4">
              {[...Array(3)].map((_, i) => (
                <Skeleton key={i} className="h-16 w-full rounded-2xl" />
              ))}
            </div>
          ) : !ordersPage || ordersPage.content.length === 0 ? (
            <EmptyBlock message={t("noOrdersYet")} />
          ) : (
            <div className="divide-y divide-ink/8 lg:divide-y-0">
              {ordersPage.content.map((order, i) => (
                <div
                  key={order.id}
                  className={`py-3 lg:py-0 ${i === 0 ? "first:pt-0" : "lg:pt-4 lg:mt-4 lg:border-t lg:border-ink/8"}`}
                >
                  <OverviewOrderRow order={order} />
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Vouchers — coming soon */}
        <div className="col-span-12 xl:col-span-5">
          <section className="rounded-3xl bg-white border border-ink/8 p-4 lg:p-6 h-full">
            <h2 className="font-black text-lg lg:text-xl mb-4">{t("yourVouchers")}</h2>
            <EmptyBlock message={t("noVouchersYet")} />
          </section>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Addresses preview — real */}
        <section className="rounded-3xl bg-soft p-5 lg:p-6">
          <div className="flex items-baseline justify-between mb-3">
            <h2 className="font-black text-lg">{t("sectionAddresses")}</h2>
            <Link href="/profile/addresses" className="text-mag text-[12px] font-bold hover:underline">
              {t("manageAddresses")}
            </Link>
          </div>

          {addressesLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-10 w-full rounded-lg" />
              <Skeleton className="h-10 w-full rounded-lg" />
            </div>
          ) : !addresses || addresses.length === 0 ? (
            <p className="text-[13px] text-ink/55">{t("addressEmpty")}</p>
          ) : (
            <div className="space-y-3 text-[13px]">
              {addresses.slice(0, 2).map((address) => (
                <div key={address.id} className="flex items-start gap-2">
                  <span
                    className={`px-2 py-0.5 rounded-full text-[10px] font-bold mt-0.5 ${
                      address.isDefault ? "bg-mag text-white" : "bg-ink/8 text-ink/60"
                    }`}
                  >
                    {address.isDefault ? t("defaultBadge") : t(ADDRESS_LABEL_KEYS[address.label])}
                  </span>
                  <div>
                    <div className="font-bold">{t(ADDRESS_LABEL_KEYS[address.label])}</div>
                    <div className="text-ink/60 text-[12px]">
                      {address.line1} · {address.city}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Payment preview — coming soon */}
        <section className="rounded-3xl bg-soft p-5 lg:p-6">
          <div className="flex items-baseline justify-between mb-3">
            <h2 className="font-black text-lg">{t("paymentMethodsTitle")}</h2>
            <Link href="/profile/payment" className="text-mag text-[12px] font-bold hover:underline">
              {t("manageAddresses")}
            </Link>
          </div>
          <p className="text-[13px] text-ink/55">{t("noPaymentMethods")}</p>
        </section>
      </div>
    </section>
  );
}
