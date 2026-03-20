"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "next/navigation";
import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { useAuth } from "@/features/auth";
import { getMyOrders, updateProfile } from "@/features/profile/api";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/tabs";
import { Avatar, AvatarFallback } from "@/shared/ui/avatar";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import { getErrorMessage } from "@/shared/lib";
import {
  User,
  ShoppingBag,
  Star,
  Check,
  Package,
  Truck,
  CircleCheckBig,
  Clock,
  Pencil,
  X,
} from "lucide-react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import LoyaltyDashboard from "@/widgets/loyalty-dashboard/LoyaltyDashboard";

/* ── Order status steps ────────────────────────────────────────── */
const ORDER_STEPS = ["PENDING", "PAID", "SHIPPED", "DELIVERED"] as const;

const STEP_ICONS = {
  PENDING: Clock,
  PAID: Check,
  SHIPPED: Truck,
  DELIVERED: CircleCheckBig,
} as const;

const STEP_KEYS = {
  PENDING: "orderPlaced",
  PAID: "orderConfirmed",
  SHIPPED: "orderShipped",
  DELIVERED: "orderDelivered",
} as const;

function getStepIndex(status: string): number {
  const idx = ORDER_STEPS.indexOf(status as (typeof ORDER_STEPS)[number]);
  return idx === -1 ? 0 : idx;
}

/* ── Inline Editable Field ─────────────────────────────────────── */
function InlineEditField({
  label,
  value,
  placeholder,
  type = "text",
  onSave,
}: {
  label: string;
  value: string;
  placeholder: string;
  type?: string;
  onSave: (value: string) => Promise<void>;
}) {
  const [isEditing, setIsEditing] = useState(false);
  const [editValue, setEditValue] = useState(value);
  const [saving, setSaving] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setEditValue(value);
  }, [value]);

  useEffect(() => {
    if (isEditing) inputRef.current?.focus();
  }, [isEditing]);

  const handleSave = async () => {
    if (editValue === value) {
      setIsEditing(false);
      return;
    }
    setSaving(true);
    try {
      await onSave(editValue);
      setIsEditing(false);
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setEditValue(value);
    setIsEditing(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") handleSave();
    if (e.key === "Escape") handleCancel();
  };

  if (isEditing) {
    return (
      <div>
        <p className="text-xs font-medium text-muted-foreground mb-1">{label}</p>
        <div className="flex items-center gap-2">
          <Input
            ref={inputRef}
            type={type}
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            onKeyDown={handleKeyDown}
            className="h-8 text-sm"
            disabled={saving}
          />
          <Button
            size="sm"
            variant="ghost"
            className="h-8 w-8 p-0 text-primary hover:text-primary"
            onClick={handleSave}
            disabled={saving}
          >
            <Check className="h-4 w-4" />
          </Button>
          <Button
            size="sm"
            variant="ghost"
            className="h-8 w-8 p-0 text-muted-foreground"
            onClick={handleCancel}
            disabled={saving}
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div
      className="group cursor-pointer rounded-lg p-2 -m-2 hover:bg-accent/50 transition-colors"
      onClick={() => setIsEditing(true)}
    >
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      <div className="flex items-center gap-2 mt-1">
        <p className="text-sm text-foreground flex-1">
          {value || placeholder}
        </p>
        <Pencil className="h-3 w-3 text-muted-foreground md:opacity-0 md:group-hover:opacity-100 transition-opacity" />
      </div>
    </div>
  );
}

/* ── Read-only Info Field ──────────────────────────────────────── */
function InfoField({
  label,
  value,
}: {
  label: string;
  value?: string | null;
}) {
  return (
    <div className="p-2 -m-2">
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm text-foreground">{value ?? "—"}</p>
    </div>
  );
}

/* ── Order Timeline ────────────────────────────────────────────── */
function OrderTimeline({ status }: { status: string }) {
  const t = useTranslations("profile");
  const currentStep = getStepIndex(status);
  const isCancelled = status === "CANCELLED";

  if (isCancelled) {
    return (
      <div className="flex items-center gap-2 mt-3">
        <X className="h-4 w-4 text-destructive" />
        <span className="text-xs text-destructive font-medium">
          {t("statusCancelled")}
        </span>
      </div>
    );
  }

  return (
    <div className="flex items-center mt-3 w-full overflow-hidden">
      {ORDER_STEPS.map((step, i) => {
        const Icon = STEP_ICONS[step];
        const isComplete = i <= currentStep;
        const isCurrent = i === currentStep;
        return (
          <div key={step} className="flex items-center flex-1 min-w-0 last:flex-initial">
            <div className="flex flex-col items-center min-w-0">
              <div
                className={`flex items-center justify-center h-6 w-6 sm:h-7 sm:w-7 rounded-full shrink-0 transition-colors ${
                  isCurrent
                    ? "bg-primary text-primary-foreground"
                    : isComplete
                    ? "bg-primary/20 text-primary"
                    : "bg-muted text-muted-foreground"
                }`}
              >
                <Icon className="h-3 w-3 sm:h-3.5 sm:w-3.5" />
              </div>
              <span
                className={`text-[10px] sm:text-xs mt-1 text-center leading-tight truncate max-w-[60px] sm:max-w-none ${
                  isComplete ? "text-foreground" : "text-muted-foreground"
                }`}
              >
                {t(STEP_KEYS[step])}
              </span>
            </div>
            {i < ORDER_STEPS.length - 1 && (
              <div
                className={`h-0.5 flex-1 min-w-2 mx-0.5 rounded-full mt-[-14px] ${
                  i < currentStep ? "bg-primary/40" : "bg-muted"
                }`}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}

/* ── Main Profile Page ─────────────────────────────────────────── */
export default function ProfilePage() {
  const t = useTranslations("profile");
  const { user, updateUser } = useAuth();
  const searchParams = useSearchParams();
  const defaultTab = searchParams.get("tab") || "account";

  const {
    data: orders = [],
    isLoading: loadingOrders,
  } = useQuery({
    queryKey: ["my-orders"],
    queryFn: getMyOrders,
  });

  const handleFieldSave = useCallback(
    async (field: "name" | "email", value: string) => {
      const data = {
        name: user?.name || "",
        email: user?.email || "",
        [field]: value,
      };
      try {
        const updatedUser = await updateProfile(data);
        updateUser(updatedUser);
        toast.success(t("profileUpdated"));
      } catch (err: unknown) {
        toast.error(t("failedToUpdateProfile") + " " + getErrorMessage(err));
        throw err;
      }
    },
    [user, updateUser, t],
  );

  const initials = user?.name
    ? user.name
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : user?.phone?.slice(-2) ?? "?";

  const loyalty = user?.loyalty;
  const points = loyalty?.points ?? 0;

  const avatarRing =
    user?.role === "MANAGER" || user?.role === "ADMIN"
      ? "ring-purple-400"
      : "ring-primary/30";

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-muted/30 py-10">
        <div className="max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8">
          {/* Profile Header */}
          <div className="bg-gradient-to-r from-primary/5 via-background to-primary/5 rounded-xl border shadow-sm p-6 sm:p-8 mb-6">
            <div className="flex items-center gap-5">
              <Avatar className={`h-16 w-16 text-lg ring-[3px] ${avatarRing} ring-offset-2 ring-offset-background`}>
                <AvatarFallback className="bg-gradient-to-br from-primary/20 to-primary/5 text-primary font-semibold">
                  {initials}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <h1 className="text-2xl font-bold text-foreground truncate">
                  {user?.name || t("yourProfile")}
                </h1>
                <p className="text-sm text-muted-foreground">{user?.phone}</p>
                {(loyalty?.tier || points > 0) && (
                  <div className="flex items-center gap-2 mt-1.5">
                    {loyalty?.tier && (
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800">
                        <Star className="h-3 w-3 fill-amber-500 text-amber-500" />
                        {loyalty.tier.name} · {loyalty.tier.discountPercentage}% {t("discount")}
                      </span>
                    )}
                    {points > 0 && (
                      <span className="inline-flex items-center gap-1 text-sm font-semibold text-foreground">
                        <Star className="h-3.5 w-3.5 fill-amber-500 text-amber-500" />
                        {t("points", { count: points })}
                      </span>
                    )}
                  </div>
                )}
              </div>
              <span
                className={`px-3 py-1 rounded-full text-xs font-semibold shrink-0 ${
                  user?.role === "MANAGER"
                    ? "bg-purple-100 text-purple-700"
                    : "bg-blue-100 text-blue-700"
                }`}
              >
                {user?.role}
              </span>
            </div>
          </div>

          {/* Tabbed sections */}
          <Tabs defaultValue={defaultTab} className="space-y-6">
            <TabsList className="w-full sm:w-auto">
              <TabsTrigger value="account" className="gap-1.5">
                <User className="h-4 w-4" />
                {t("tabAccount")}
              </TabsTrigger>
              <TabsTrigger value="orders" className="gap-1.5">
                <ShoppingBag className="h-4 w-4" />
                {t("tabOrders")}
              </TabsTrigger>
              <TabsTrigger value="loyalty" className="gap-1.5">
                <Star className="h-4 w-4" />
                {t("tabLoyalty")}
              </TabsTrigger>
            </TabsList>

            {/* Account Tab — inline editing */}
            <TabsContent value="account">
              <div className="bg-card rounded-xl border shadow-sm p-6 sm:p-8">
                <h2 className="text-lg font-semibold text-foreground mb-6">
                  {t("accountInformation")}
                </h2>
                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                  <InfoField label={t("phoneNumber")} value={user?.phone} />
                  <InlineEditField
                    label={t("nameLabel")}
                    value={user?.name || ""}
                    placeholder={t("notSet")}
                    onSave={(v) => handleFieldSave("name", v)}
                  />
                  <InlineEditField
                    label={t("emailLabel")}
                    value={user?.email || ""}
                    placeholder={t("notSet")}
                    type="email"
                    onSave={(v) => handleFieldSave("email", v)}
                  />
                  <InfoField
                    label={t("userId")}
                    value={user?.id?.toString()}
                  />
                </div>
              </div>
            </TabsContent>

            {/* Orders Tab — with timeline */}
            <TabsContent value="orders">
              <div className="bg-card rounded-xl border shadow-sm p-6 sm:p-8">
                <h2 className="text-lg font-semibold text-foreground mb-6">
                  {t("orderHistory")}
                </h2>
                {loadingOrders ? (
                  <div className="space-y-4">
                    {Array.from({ length: 3 }).map((_, i) => (
                      <Skeleton key={i} className="h-32 w-full rounded-lg" />
                    ))}
                  </div>
                ) : orders.length === 0 ? (
                  <div className="text-center py-12">
                    <Package className="h-12 w-12 text-muted-foreground/40 mx-auto mb-3" />
                    <p className="text-muted-foreground text-sm">
                      {t("noOrdersFound")}
                    </p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {orders.map((order) => (
                      <div
                        key={order.id}
                        className="border rounded-xl p-4 hover:border-primary/20 transition-colors"
                      >
                        <div className="flex justify-between items-start mb-1">
                          <div className="flex items-center gap-2">
                            <span className="font-medium text-sm">
                              {t("orderNumber", { id: order.id })}
                            </span>
                            <span
                              className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                                order.status === "PENDING"
                                  ? "bg-yellow-100 text-yellow-800"
                                  : order.status === "DELIVERED"
                                  ? "bg-green-100 text-green-800"
                                  : order.status === "CANCELLED"
                                  ? "bg-red-100 text-red-800"
                                  : order.status === "SHIPPED"
                                  ? "bg-blue-100 text-blue-800"
                                  : "bg-gray-100 text-gray-800"
                              }`}
                            >
                              {order.status}
                            </span>
                          </div>
                          <span className="text-xs text-muted-foreground">
                            {new Date(order.createdAt).toLocaleDateString()}
                          </span>
                        </div>
                        <ul className="text-sm text-muted-foreground mb-1 space-y-0.5">
                          {order.items.map((i, idx) => (
                            <li key={idx}>
                              {i.productName} <span className="text-xs">x{i.quantity}</span>
                            </li>
                          ))}
                        </ul>
                        <OrderTimeline status={order.status} />
                        <p className="font-semibold text-sm text-right mt-3 pt-3 border-t">
                          {t("total")} {order.totalAmount.toFixed(2)} TJS
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </TabsContent>

            {/* Loyalty Tab */}
            <TabsContent value="loyalty">
              {loyalty && <LoyaltyDashboard loyalty={loyalty} />}
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </ProtectedRoute>
  );
}
