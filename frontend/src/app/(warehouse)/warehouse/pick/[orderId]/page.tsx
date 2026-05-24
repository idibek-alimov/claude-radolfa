import { getTranslations } from "next-intl/server";

export default async function PickSessionPage() {
  const t = await getTranslations("warehouse");
  return <p className="text-sm text-muted-foreground">{t("common.placeholderBody")}</p>;
}
