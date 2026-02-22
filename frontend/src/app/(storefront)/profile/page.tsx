"use client";

import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { useAuth } from "@/features/auth";
import { getMyOrders, updateProfile } from "@/features/profile/api";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/tabs";
import { Avatar, AvatarFallback } from "@/shared/ui/avatar";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import { getErrorMessage } from "@/shared/lib";
import { User, ShoppingBag, Star, AlertCircle } from "lucide-react";
import { useTranslations } from "next-intl";

export default function ProfilePage() {
  const t = useTranslations("profile");
  const { user, updateUser } = useAuth();

  // Edit Form
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({ name: "", email: "" });
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");

  // Orders via useQuery
  const {
    data: orders = [],
    isLoading: loadingOrders,
  } = useQuery({
    queryKey: ["my-orders"],
    queryFn: getMyOrders,
  });

  useEffect(() => {
    if (user) {
      setFormData({ name: user.name || "", email: user.email || "" });
    }
  }, [user]);

  const handleSaveProfile = async () => {
    setSaving(true);
    setSaveError("");
    try {
      const updatedUser = await updateProfile(formData);
      updateUser(updatedUser);
      setIsEditing(false);
    } catch (err: unknown) {
      console.error(err);
      setSaveError(t("failedToUpdateProfile") + " " + getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const initials = user?.name
    ? user.name
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : user?.phone?.slice(-2) ?? "?";

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-muted/30 py-10">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Profile Header */}
          <div className="bg-card rounded-xl border shadow-sm p-6 sm:p-8 mb-6">
            <div className="flex items-center gap-5">
              <Avatar className="h-16 w-16 text-lg">
                <AvatarFallback className="bg-primary/10 text-primary font-semibold">
                  {initials}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1">
                <h1 className="text-2xl font-bold text-foreground">
                  {user?.name || t("yourProfile")}
                </h1>
                <p className="text-sm text-muted-foreground">{user?.phone}</p>
              </div>
              <span
                className={`px-3 py-1 rounded-full text-xs font-semibold ${
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
          <Tabs defaultValue="account" className="space-y-6">
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

            {/* Account Tab */}
            <TabsContent value="account">
              <div className="bg-card rounded-xl border shadow-sm p-6 sm:p-8">
                <div className="flex justify-between items-center mb-6">
                  <h2 className="text-lg font-semibold text-foreground">
                    {t("accountInformation")}
                  </h2>
                  {!isEditing && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setIsEditing(true)}
                    >
                      {t("edit")}
                    </Button>
                  )}
                </div>

                {isEditing ? (
                  <div className="space-y-4 max-w-md">
                    <div className="space-y-2">
                      <label className="text-sm font-medium text-foreground">
                        {t("nameLabel")}
                      </label>
                      <Input
                        value={formData.name}
                        onChange={(e) =>
                          setFormData({ ...formData, name: e.target.value })
                        }
                      />
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium text-foreground">
                        {t("emailLabel")}
                      </label>
                      <Input
                        type="email"
                        value={formData.email}
                        onChange={(e) =>
                          setFormData({ ...formData, email: e.target.value })
                        }
                      />
                    </div>
                    {saveError && (
                      <div className="flex items-center gap-2 text-destructive text-sm">
                        <AlertCircle className="h-4 w-4" />
                        {saveError}
                      </div>
                    )}
                    <div className="flex gap-3 pt-2">
                      <Button
                        onClick={handleSaveProfile}
                        disabled={saving}
                        size="sm"
                      >
                        {saving ? t("saving") : t("saveChanges")}
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setIsEditing(false)}
                      >
                        {t("cancel")}
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <InfoField label={t("phoneNumber")} value={user?.phone} />
                    <InfoField
                      label={t("nameLabel")}
                      value={user?.name || t("notSet")}
                    />
                    <InfoField
                      label={t("emailLabel")}
                      value={user?.email || t("notSet")}
                    />
                    <InfoField
                      label={t("userId")}
                      value={user?.id?.toString()}
                    />
                  </div>
                )}
              </div>
            </TabsContent>

            {/* Orders Tab */}
            <TabsContent value="orders">
              <div className="bg-card rounded-xl border shadow-sm p-6 sm:p-8">
                <h2 className="text-lg font-semibold text-foreground mb-6">
                  {t("orderHistory")}
                </h2>
                {loadingOrders ? (
                  <div className="space-y-4">
                    {Array.from({ length: 3 }).map((_, i) => (
                      <Skeleton key={i} className="h-24 w-full rounded-lg" />
                    ))}
                  </div>
                ) : orders.length === 0 ? (
                  <p className="text-muted-foreground text-sm py-8 text-center">
                    {t("noOrdersFound")}
                  </p>
                ) : (
                  <div className="space-y-4">
                    {orders.map((order) => (
                      <div
                        key={order.id}
                        className="border rounded-lg p-4"
                      >
                        <div className="flex justify-between items-center mb-2">
                          <div className="flex items-center gap-2">
                            <span className="font-medium text-sm">
                              {t("orderNumber", { id: order.id })}
                            </span>
                            <span
                              className={`text-xs px-2 py-0.5 rounded-full ${
                                order.status === "PENDING"
                                  ? "bg-yellow-100 text-yellow-800"
                                  : order.status === "DELIVERED"
                                  ? "bg-green-100 text-green-800"
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
                        <p className="text-sm text-muted-foreground mb-2">
                          {order.items
                            .map(
                              (i) => `${i.productName} (x${i.quantity})`
                            )
                            .join(", ")}
                        </p>
                        <p className="font-semibold text-sm text-right">
                          {t("total")} ${order.totalAmount}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </TabsContent>

            {/* Loyalty Tab */}
            <TabsContent value="loyalty">
              <div className="bg-card rounded-xl border shadow-sm p-6 sm:p-8">
                <h2 className="text-lg font-semibold text-foreground mb-6">
                  {t("loyaltyPoints")}
                </h2>
                <div className="bg-gradient-to-r from-indigo-500 to-purple-600 rounded-xl p-6 text-white">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm opacity-90">{t("availablePoints")}</p>
                      <p className="text-4xl font-bold mt-1">
                        {user?.loyaltyPoints ?? 0}
                      </p>
                      <p className="text-xs opacity-75 mt-2">
                        {t("earnPoints")}
                      </p>
                    </div>
                    <Star className="h-14 w-14 opacity-50" />
                  </div>
                </div>
              </div>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </ProtectedRoute>
  );
}

function InfoField({
  label,
  value,
}: {
  label: string;
  value?: string | null;
}) {
  return (
    <div>
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm text-foreground">{value ?? "—"}</p>
    </div>
  );
}
