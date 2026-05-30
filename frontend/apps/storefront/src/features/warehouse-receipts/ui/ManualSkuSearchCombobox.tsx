"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ChevronsUpDown, Loader2 } from "lucide-react";
import { Button } from "@/shared/ui/button";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/shared/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/shared/ui/popover";
import { useDebounce } from "@/shared/lib";
import { useSearchSkus } from "@/entities/warehouse-sku";
import type { SkuLookupResponse } from "@/entities/warehouse-sku";

interface Props {
  onSelect: (sku: SkuLookupResponse) => void;
}

export function ManualSkuSearchCombobox({ onSelect }: Props) {
  const t = useTranslations("warehouse");
  const [open, setOpen] = useState(false);
  const [inputValue, setInputValue] = useState("");
  const debouncedQuery = useDebounce(inputValue, 300);

  const { data, isFetching } = useSearchSkus(debouncedQuery, 1);

  function handleSelect(sku: SkuLookupResponse) {
    onSelect(sku);
    setOpen(false);
    setInputValue("");
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className="w-full justify-between font-normal"
        >
          <span className="text-muted-foreground">{t("receipts.create.manualSearchPlaceholder")}</span>
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[500px] p-0" align="start">
        <Command shouldFilter={false}>
          <CommandInput
            placeholder={t("receipts.create.manualSearchPlaceholder")}
            value={inputValue}
            onValueChange={setInputValue}
          />
          <CommandList>
            {isFetching && (
              <div className="flex items-center justify-center py-4 text-sm text-muted-foreground gap-2">
                <Loader2 className="h-4 w-4 animate-spin" />
                {t("common.loading")}
              </div>
            )}
            {!isFetching && debouncedQuery.trim().length < 2 && (
              <div className="py-4 text-center text-sm text-muted-foreground">
                {t("receipts.create.manualSearchHint")}
              </div>
            )}
            {!isFetching && debouncedQuery.trim().length >= 2 && (
              <>
                {(!data || data.content.length === 0) ? (
                  <CommandEmpty>{t("receipts.create.manualNoResults")}</CommandEmpty>
                ) : (
                  <CommandGroup>
                    {data.content.map((sku) => (
                      <CommandItem
                        key={sku.skuId}
                        value={String(sku.skuId)}
                        onSelect={() => handleSelect(sku)}
                        className="cursor-pointer"
                      >
                        <div className="flex flex-col gap-0.5">
                          <span className="font-medium text-sm">{sku.productName}</span>
                          <span className="text-xs text-muted-foreground font-mono">
                            {sku.skuCode} · {sku.sizeLabel}
                          </span>
                        </div>
                      </CommandItem>
                    ))}
                  </CommandGroup>
                )}
              </>
            )}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
