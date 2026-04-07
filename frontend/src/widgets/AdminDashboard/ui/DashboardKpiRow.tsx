"use client";

import { useQuery } from "@tanstack/react-query";
import { Package, Users, Percent, ShieldAlert } from "lucide-react";
import { fetchListings } from "@/entities/product/api";
import { fetchDiscounts } from "@/features/discount-management/api";
import { fetchUsers } from "@/features/user-management";
import { fetchPendingReviews } from "@/entities/review";
import { fetchPendingQuestions } from "@/entities/question";
import { cn } from "@/shared/lib";
import type { LucideIcon } from "lucide-react";

interface KpiCardProps {
  label: string;
  value: number | undefined;
  icon: LucideIcon;
  accentClass: string;
  iconBg: string;
  iconColor: string;
}

function KpiCard({ label, value, icon: Icon, accentClass, iconBg, iconColor }: KpiCardProps) {
  return (
    <div className="relative bg-white rounded-xl border border-zinc-100 shadow-sm p-5 overflow-hidden hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
      {/* Left accent strip */}
      <div className={cn("absolute left-0 top-0 bottom-0 w-[3px] rounded-l-xl", accentClass)} />

      <div className="flex items-start justify-between pl-2">
        <div className="min-w-0">
          <p className="text-[11px] font-semibold uppercase tracking-widest text-zinc-400">
            {label}
          </p>
          {value !== undefined ? (
            <p className="mt-2 text-3xl font-bold tabular-nums text-zinc-900 leading-none">
              {value.toLocaleString()}
            </p>
          ) : (
            <div className="mt-2.5 h-7 w-20 rounded-lg bg-zinc-100 animate-pulse" />
          )}
        </div>
        <div className={cn("flex h-10 w-10 shrink-0 items-center justify-center rounded-xl", iconBg)}>
          <Icon className={cn("h-5 w-5", iconColor)} strokeWidth={1.75} />
        </div>
      </div>
    </div>
  );
}

export function DashboardKpiRow() {
  const { data: productStats } = useQuery({
    queryKey: ["listings-count"],
    queryFn: () => fetchListings(1, 1),
    staleTime: 60_000,
  });

  const { data: userStats } = useQuery({
    queryKey: ["users-count"],
    queryFn: () => fetchUsers("", 1, 1),
    staleTime: 60_000,
  });

  const { data: activeDiscountStats } = useQuery({
    queryKey: ["discounts-count-active"],
    queryFn: () => fetchDiscounts({ status: "active", page: 1, size: 1 }),
    staleTime: 60_000,
  });

  const { data: pendingReviews } = useQuery({
    queryKey: ["pending-reviews"],
    queryFn: fetchPendingReviews,
    staleTime: 30_000,
  });

  const { data: pendingQuestions } = useQuery({
    queryKey: ["pending-questions"],
    queryFn: fetchPendingQuestions,
    staleTime: 30_000,
  });

  const pendingModerationCount =
    pendingReviews !== undefined && pendingQuestions !== undefined
      ? pendingReviews.length + pendingQuestions.length
      : undefined;

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <KpiCard
        label="Total Products"
        value={productStats?.totalElements}
        icon={Package}
        accentClass="bg-blue-500"
        iconBg="bg-blue-50"
        iconColor="text-blue-500"
      />
      <KpiCard
        label="Registered Users"
        value={userStats?.totalElements}
        icon={Users}
        accentClass="bg-violet-500"
        iconBg="bg-violet-50"
        iconColor="text-violet-500"
      />
      <KpiCard
        label="Active Campaigns"
        value={activeDiscountStats?.totalElements}
        icon={Percent}
        accentClass="bg-emerald-500"
        iconBg="bg-emerald-50"
        iconColor="text-emerald-500"
      />
      <KpiCard
        label="Pending Moderation"
        value={pendingModerationCount}
        icon={ShieldAlert}
        accentClass="bg-rose-500"
        iconBg="bg-rose-50"
        iconColor="text-rose-500"
      />
    </div>
  );
}
