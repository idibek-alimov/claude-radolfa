import { useTranslations } from "next-intl";

interface DescriptionBlockProps {
  webDescription: string | null;
}

/**
 * Real `webDescription` styled in the reference's "Material story" soft shell.
 * The dark-gradient "What fits inside" poster and the Crown promo card are
 * deliberately omitted — see updates/pdp/01-pdp-redesign.md Phase 4.
 */
export default function DescriptionBlock({ webDescription }: DescriptionBlockProps) {
  const t = useTranslations("productDetail");

  if (!webDescription) return null;

  return (
    <div className="rounded-2xl md:rounded-3xl bg-soft p-5 md:p-6">
      <div className="text-[10px] tracking-[0.2em] uppercase font-bold text-mag">
        {t("aboutProduct")}
      </div>
      <p className="text-[13px] text-ink/70 mt-3 leading-relaxed whitespace-pre-line">
        {webDescription}
      </p>
    </div>
  );
}
