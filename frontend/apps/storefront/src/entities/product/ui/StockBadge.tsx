import { Badge } from "@/shared/ui/badge";
import { useTranslations } from "next-intl";

interface StockBadgeProps {
  stock: number | null;
}

export default function StockBadge({ stock }: StockBadgeProps) {
  const t = useTranslations("common");
  const inStock = (stock ?? 0) > 0;

  return (
    <Badge variant={inStock ? "success" : "destructive"} className="whitespace-nowrap text-[10px] sm:text-xs">
      {inStock ? t("inStock", { count: stock ?? 0 }) : t("outOfStock")}
    </Badge>
  );
}
