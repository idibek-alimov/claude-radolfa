"use client";

import { useTranslations } from "next-intl";
import { Trash2 } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
import { Input } from "@/shared/ui/input";
import { Button } from "@/shared/ui/button";
import type { LineItem } from "../types";

interface EditableProps {
  readonly?: false;
  items: LineItem[];
  onQuantityChange: (skuId: number, quantity: number) => void;
  onNotesChange: (skuId: number, notes: string) => void;
  onRemove: (skuId: number) => void;
}

interface ReadonlyProps {
  readonly: true;
  items: { skuCode: string; productName: string; quantityReceived: number; notes: string | null }[];
}

type Props = EditableProps | ReadonlyProps;

export function ReceiptLineItemsTable(props: Props) {
  const t = useTranslations("warehouse");

  if (props.readonly) {
    return (
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t("receipts.create.lineItems.product")}</TableHead>
            <TableHead>{t("receipts.create.lineItems.skuCode")}</TableHead>
            <TableHead className="text-right">{t("receipts.detail.columns.qty")}</TableHead>
            <TableHead>{t("receipts.create.lineItems.notes")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {props.items.map((item, i) => (
            <TableRow key={i}>
              <TableCell className="font-medium">{item.productName}</TableCell>
              <TableCell>
                <span className="font-mono text-xs bg-zinc-100 px-1.5 py-0.5 rounded">
                  {item.skuCode}
                </span>
              </TableCell>
              <TableCell className="text-right font-bold tabular-nums">
                {item.quantityReceived}
              </TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {item.notes ?? "—"}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t("receipts.create.lineItems.product")}</TableHead>
          <TableHead>{t("receipts.create.lineItems.skuCode")}</TableHead>
          <TableHead className="w-28">{t("receipts.create.lineItems.quantity")}</TableHead>
          <TableHead>{t("receipts.create.lineItems.notes")}</TableHead>
          <TableHead className="w-10" />
        </TableRow>
      </TableHeader>
      <TableBody>
        {props.items.map((item) => (
          <TableRow key={item.skuId}>
            <TableCell>
              <p className="font-medium text-sm">{item.productName}</p>
              <p className="text-xs text-muted-foreground">{item.sizeLabel}</p>
            </TableCell>
            <TableCell>
              <span className="font-mono text-xs bg-zinc-100 px-1.5 py-0.5 rounded">
                {item.skuCode}
              </span>
            </TableCell>
            <TableCell>
              <Input
                type="number"
                min={1}
                value={item.quantity}
                onChange={(e) =>
                  props.onQuantityChange(item.skuId, Number(e.target.value))
                }
                onBlur={(e) => {
                  const v = Math.max(1, Number(e.target.value));
                  props.onQuantityChange(item.skuId, v);
                }}
                className="w-20 h-8 text-sm"
              />
            </TableCell>
            <TableCell>
              <Input
                value={item.notes}
                onChange={(e) => props.onNotesChange(item.skuId, e.target.value)}
                placeholder="—"
                className="h-8 text-sm"
              />
            </TableCell>
            <TableCell>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => props.onRemove(item.skuId)}
                className="text-rose-600 hover:text-rose-700 hover:bg-rose-50 h-8 w-8 p-0"
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
