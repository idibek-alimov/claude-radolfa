"use client";

import Image from "next/image";
import { useTranslations } from "next-intl";
import { Package } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
import type { AdminOrderItem } from "@/entities/order";

function StockBadge({ item }: { item: AdminOrderItem }) {
  const t = useTranslations("manage.orders.stock");
  if (item.currentStock === null) {
    return <span className="text-muted-foreground text-xs">—</span>;
  }
  if (item.currentStock >= item.quantity) {
    return <span className="text-green-600 text-xs font-medium">{t("inStock")}</span>;
  }
  return (
    <span className="text-rose-600 text-xs font-medium">
      {t("shortage", { count: item.currentStock })}
    </span>
  );
}

interface Props {
  items: AdminOrderItem[];
}

export function OrderItemsStockTable({ items }: Props) {
  const t = useTranslations("manage.orders.itemsTable");

  return (
    <div className="rounded-xl border bg-card overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="pl-4">{t("product")}</TableHead>
            <TableHead className="text-center">{t("qty")}</TableHead>
            <TableHead className="text-right">{t("unitPrice")}</TableHead>
            <TableHead className="text-right">{t("subtotal")}</TableHead>
            <TableHead className="text-right pr-4">{t("stock")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((item, i) => (
            <TableRow key={i}>
              <TableCell className="pl-4">
                <div className="flex items-center gap-3">
                  {item.imageUrl ? (
                    <Image
                      src={item.imageUrl}
                      alt={item.productName}
                      width={40}
                      height={40}
                      unoptimized
                      className="rounded-md object-cover w-10 h-10 border bg-muted shrink-0"
                    />
                  ) : (
                    <div className="w-10 h-10 rounded-md bg-muted border shrink-0 flex items-center justify-center">
                      <Package className="h-4 w-4 text-muted-foreground" />
                    </div>
                  )}
                  <div className="min-w-0">
                    <p className="text-sm font-medium truncate">{item.productName}</p>
                    {item.sizeLabel && (
                      <p className="text-xs text-muted-foreground">{item.sizeLabel}</p>
                    )}
                  </div>
                </div>
              </TableCell>
              <TableCell className="text-center text-sm">{item.quantity}</TableCell>
              <TableCell className="text-right text-sm tabular-nums">
                {item.price.toFixed(2)} TJS
              </TableCell>
              <TableCell className="text-right text-sm font-semibold tabular-nums">
                {(item.price * item.quantity).toFixed(2)} TJS
              </TableCell>
              <TableCell className="text-right pr-4">
                <StockBadge item={item} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
