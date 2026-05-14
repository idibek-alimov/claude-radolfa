"use client";

import { useQuery } from "@tanstack/react-query";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { useDebounce } from "@/shared/lib";
import { fetchCouponAvailable } from "../../api";
import type { DiscountWizardState } from "../DiscountCreationWizard";
import { Tag, CheckCircle2, XCircle, Loader2 } from "lucide-react";

interface Props {
  state: DiscountWizardState;
  update: (patch: Partial<DiscountWizardState>) => void;
  sourceId?: number;
  submitted: boolean;
}

export function Step2bCoupon({ state, update, sourceId, submitted }: Props) {
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
    <div className="space-y-8">
      <div>
        <div className="flex items-center gap-2 pb-2 border-b border-border mb-6">
          <Tag className="h-4 w-4 text-muted-foreground" />
          <h3 className="text-sm font-semibold text-foreground">Coupon Code</h3>
        </div>
        <p className="text-sm text-muted-foreground mb-6">
          Optional — leave blank for a public discount available to all shoppers. When set, this discount is hidden from product listings and only activates when the shopper enters the code at checkout.
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
                <span className="text-destructive text-xs">Invalid format — use A–Z and 0–9 only, 3–32 characters.</span>
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
      </div>
    </div>
  );
}
