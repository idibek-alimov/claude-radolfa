import { Badge } from "@/shared/ui/badge";

interface StockBadgeProps {
  stock: number | null;
}

export default function StockBadge({ stock }: StockBadgeProps) {
  const inStock = (stock ?? 0) > 0;

  return (
    <Badge variant={inStock ? "success" : "destructive"}>
      {inStock ? `${stock} in stock` : "Out of stock"}
    </Badge>
  );
}
