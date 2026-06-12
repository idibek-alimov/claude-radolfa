import { Truck, ShieldCheck, BadgeCheck, HeartHandshake } from "lucide-react";
import { getTranslations } from "next-intl/server";
import type { ElementType } from "react";

export default async function TrustBanner() {
  const t = await getTranslations("trustBanner");

  const TRUST_ITEMS: { icon: ElementType; title: string; description: string }[] = [
    {
      icon: Truck,
      title: t("fastDelivery.title"),
      description: t("fastDelivery.description"),
    },
    {
      icon: ShieldCheck,
      title: t("securePayments.title"),
      description: t("securePayments.description"),
    },
    {
      icon: BadgeCheck,
      title: t("qualityGuarantee.title"),
      description: t("qualityGuarantee.description"),
    },
    {
      icon: HeartHandshake,
      title: t("customerSupport.title"),
      description: t("customerSupport.description"),
    },
  ];

  return (
    <section className="border-y bg-muted/50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-8">
          {TRUST_ITEMS.map((item) => (
            <div key={item.title} className="flex flex-col items-center text-center gap-3">
              <div className="flex items-center justify-center w-12 h-12 rounded-full bg-primary/10">
                <item.icon className="h-6 w-6 text-primary" />
              </div>
              <div>
                <h3 className="font-semibold text-sm text-foreground">
                  {item.title}
                </h3>
                <p className="text-xs text-muted-foreground mt-1">
                  {item.description}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
