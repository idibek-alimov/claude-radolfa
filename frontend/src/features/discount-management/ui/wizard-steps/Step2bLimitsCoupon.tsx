"use client";

import { useQuery } from "@tanstack/react-query";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { useDebounce } from "@/shared/lib";
import { fetchCouponAvailable } from "../../api";
import type { DiscountWizardState } from "../DiscountCreationWizard";
import { Tag, CheckCircle2, XCircle, Loader2, SlidersHorizontal } from "lucide-react";

interface Props {
  state: DiscountWizardState;
  update: (patch: Partial<DiscountWizardState>) => void;
  sourceId?: number;
  submitted: boolean;
}

function SectionHeading({ icon: Icon, title }: { icon: React.ElementType; title: string }) {
  return (
    <div className="flex items-center gap-2 pb-2 border-b border-border">
      <Icon className="h-4 w-4 text-muted-foreground" />
      <h3 className="text-sm font-semibold text-foreground">{title}</h3>
    </div>
  );
}

function NumericField({
  id, label, value, onChange, placeholder, suffix,
}: {
  id: string; label: string; value: string;
  onChange: (v: string) => void; placeholder?: string; suffix?: string;
}) {
  return (
    <div className="space-y-1.5">
      <Label htmlFor={id}>{label}</Label>
      <div className="flex items-center gap-2">
        <Input
          id={id}
          type="number"
          min={0}
          step="1"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className="h-9"
        />
        {suffix && <span className="text-sm text-muted-foreground shrink-0">{suffix}</span>}
      </div>
    </div>
  );
}

export function Step2bLimitsCoupon({ state, update, sourceId, submitted }: Props) {
  const raw = state.couponCode ?? "";
  const debounced = useDebounce(raw.trim(), 300);
  const shouldCheck = debounced.length >= 3;

  const { data, isFetching } = useQuery({
    queryKey: ["coupon-available", debounced, sourceId],
    queryFn: () => fetchCouponAvailable(debounced, sourceId),
    enabled: shouldCheck,
    staleTime: 5000,
  });

  const isInvalidFormat = raw.length > 0 && !/^[A-Z0-9]{3,32}$/.test(raw);

  return (
    <div className="flex-1 flex flex-col space-y-10">
      {/* Heading */}
      <div>
        <h2 className="text-xl font-semibold tracking-tight">Limits &amp; Coupon</h2>
        <p className="text-sm text-muted-foreground mt-1">
          All fields are optional. Leave blank to apply no restrictions.
        </p>
      </div>

      {/* Limits section */}
      <section className="space-y-5">
        <SectionHeading icon={SlidersHorizontal} title="Usage Limits" />
        <p className="text-sm text-muted-foreground">
          Set gates that are evaluated at checkout. Leave blank for unlimited.
        </p>
        <div className="grid grid-cols-3 gap-5 max-w-2xl">
          <NumericField
            id="minBasketAmount"
            label="Min basket amount"
            value={state.minBasketAmount}
            onChange={(v) => update({ minBasketAmount: v })}
            placeholder="0"
            suffix="TJS"
          />
          <NumericField
            id="usageCapTotal"
            label="Max uses (total)"
            value={state.usageCapTotal}
            onChange={(v) => update({ usageCapTotal: v })}
            placeholder="Unlimited"
          />
          <NumericField
            id="usageCapPerCustomer"
            label="Max uses per customer"
            value={state.usageCapPerCustomer}
            onChange={(v) => update({ usageCapPerCustomer: v })}
            placeholder="Unlimited"
          />
        </div>
      </section>

      {/* Coupon code section */}
      <section className="space-y-5">
        <SectionHeading icon={Tag} title="Coupon Code" />
        <p className="text-sm text-muted-foreground">
          Optional — leave blank for a public discount. When set, this discount is hidden from
          product listings and only activates when the shopper enters the code at checkout.
        </p>

        <div className="bg-muted/30 rounded-xl p-5 space-y-4 max-w-md">
          <div className="space-y-2">
            <Label htmlFor="coupon-code">Coupon code</Label>
            <Input
              id="coupon-code"
              placeholder="e.g. SUMMER25"
              value={raw}
              onChange={(e) =>
                update({ couponCode: e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, "") })
              }
              className="font-mono tracking-widest"
              maxLength={32}
            />
            <p className="text-xs text-muted-foreground">
              3–32 uppercase letters and digits only.
            </p>
          </div>

          {/* Live availability indicator */}
          {raw.length > 0 && (
            <div className="flex items-center gap-2 text-sm">
              {isInvalidFormat ? (
                <span className="text-destructive text-xs">
                  Invalid format — use A–Z and 0–9 only, 3–32 characters.
                </span>
              ) : isFetching ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                  <span className="text-muted-foreground">Checking…</span>
                </>
              ) : shouldCheck && data !== undefined ? (
                data.available ? (
                  <>
                    <CheckCircle2 className="h-4 w-4 text-green-600" />
                    <span className="text-green-600 font-medium">Available</span>
                  </>
                ) : (
                  <>
                    <XCircle className="h-4 w-4 text-destructive" />
                    <span className="text-destructive font-medium">Already taken</span>
                  </>
                )
              ) : null}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
