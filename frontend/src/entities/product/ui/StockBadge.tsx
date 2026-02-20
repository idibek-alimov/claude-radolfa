import { Badge } from "@/shared/ui/badge";

interface StockBadgeProps {
  stock: number | null;
}

export default function StockBadge({ stock }: StockBadgeProps) {
  const qty = stock ?? 0;

  if (qty === 0) return <Badge variant="destructive">Out of Stock</Badge>;
  if (qty <= 5) return <Badge variant="warning">Low Stock</Badge>;
  return <Badge variant="success">In Stock</Badge>;
}
