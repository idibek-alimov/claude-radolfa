"use client";

import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { getErrorMessage } from "@radolfa/shared/lib";
import { useHideOnScroll } from "@radolfa/shared/lib/useHideOnScroll";
import { formatPrice } from "@radolfa/shared/lib/format";
import { currentAppLoginPath } from "@radolfa/shared/lib/appNav";
import { useAuth } from "@radolfa/shared/auth";
import type { ListingVariantDetail, Sku } from "@/entities/product";
import { useAddToCart } from "@/features/cart";
import { useResolvedPrice } from "../lib/useResolvedPrice";

interface MobileBuyBarProps {
  listing: ListingVariantDetail;
  selectedSku: Sku | null;
}

/** Sticky bottom Add-to-Bag bar (mobile only). Sits directly above `BottomNav`
 * (`bottom-14`) and uses the same `useHideOnScroll` defaults so both bars
 * slide away/reveal together on scroll. */
export default function MobileBuyBar({ listing, selectedSku }: MobileBuyBarProps) {
  const t = useTranslations("productDetail");
  const { isAuthenticated } = useAuth();
  const addToCart = useAddToCart();
  const price = useResolvedPrice(listing, selectedSku);
  const hidden = useHideOnScroll();

  const totalStock = listing.skus.reduce((acc, s) => acc + s.stockQuantity, 0);
  const allOutOfStock = totalStock === 0;
  const ctaDisabled = !selectedSku || selectedSku.stockQuantity === 0 || addToCart.isPending;

  const handleAddToCart = () => {
    if (!selectedSku) return;
    if (!isAuthenticated) {
      toast.info(t("loginToAddToCart"), {
        action: {
          label: t("login"),
          onClick: () => { window.location.href = currentAppLoginPath(); },
        },
        actionButtonStyle: { backgroundColor: "#CB11AB", color: "#fff" },
      });
      return;
    }
    addToCart.mutate(
      { skuId: selectedSku.skuId, quantity: 1 },
      { onError: (err) => toast.error(getErrorMessage(err)) },
    );
  };

  const summary = [
    formatPrice(price.effectivePrice),
    selectedSku?.sizeLabel,
    listing.colorDisplayName,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <nav
      className={`fixed bottom-14 inset-x-0 z-30 bg-white border-t border-ink/8 px-3 py-2.5 md:hidden transition-transform duration-300 ${
        hidden ? "translate-y-[calc(100%+3.5rem)]" : "translate-y-0"
      }`}
    >
      <div className="text-[10px] text-ink/55 leading-none">{summary}</div>
      {allOutOfStock ? (
        <button
          type="button"
          disabled
          className="mt-0.5 w-full h-11 rounded-full bg-mag/30 text-white font-black text-[14px] cursor-not-allowed"
        >
          {t("outOfStock")}
        </button>
      ) : (
        <button
          type="button"
          onClick={handleAddToCart}
          disabled={ctaDisabled}
          className="mt-0.5 w-full h-11 rounded-full bg-mag text-white font-black text-[14px] hover:bg-maglo disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {!selectedSku
            ? t("selectSize")
            : addToCart.isPending
              ? t("adding")
              : t("addToBag", { price: formatPrice(price.effectivePrice) })}
        </button>
      )}
    </nav>
  );
}
