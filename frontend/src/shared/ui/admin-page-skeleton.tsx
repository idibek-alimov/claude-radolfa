import { cn } from "@/shared/lib/utils";

type SkeletonVariant = "table" | "cards" | "form";

interface AdminPageSkeletonProps {
  variant?: SkeletonVariant;
  className?: string;
}

function Pulse({ className }: { className?: string }) {
  return <div className={cn("animate-pulse rounded-lg bg-zinc-100", className)} />;
}

function PageHeaderSkeleton() {
  return (
    <div className="space-y-2 mb-6">
      <Pulse className="h-7 w-48" />
      <Pulse className="h-4 w-72" />
    </div>
  );
}

function TableSkeleton() {
  return (
    <div className="bg-white rounded-xl border border-zinc-100 shadow-sm overflow-hidden">
      {/* Toolbar row */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-zinc-100 bg-zinc-50/60">
        <Pulse className="h-8 w-56" />
        <div className="ml-auto flex gap-2">
          <Pulse className="h-8 w-24" />
          <Pulse className="h-8 w-28" />
        </div>
      </div>
      {/* Column headers */}
      <div className="grid grid-cols-4 gap-4 px-4 py-2.5 border-b border-zinc-100">
        {[1, 2, 3, 4].map((i) => (
          <Pulse key={i} className="h-3 w-16" />
        ))}
      </div>
      {/* Rows */}
      {Array.from({ length: 8 }).map((_, i) => (
        <div
          key={i}
          className="grid grid-cols-4 gap-4 px-4 py-3.5 border-b border-zinc-50 last:border-0"
        >
          <div className="flex items-center gap-3">
            <Pulse className="h-8 w-8 rounded-lg shrink-0" />
            <Pulse className="h-3.5 w-28" />
          </div>
          <Pulse className="h-3.5 w-20 self-center" />
          <Pulse className="h-5 w-16 rounded-full self-center" />
          <Pulse className="h-3.5 w-12 self-center justify-self-end" />
        </div>
      ))}
    </div>
  );
}

function CardsSkeleton() {
  return (
    <div className="space-y-6">
      {/* KPI cards row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className="bg-white rounded-xl border border-zinc-100 shadow-sm p-5 flex items-center gap-4"
          >
            <Pulse className="h-10 w-10 rounded-xl shrink-0" />
            <div className="space-y-2 flex-1">
              <Pulse className="h-3 w-20" />
              <Pulse className="h-7 w-16" />
            </div>
          </div>
        ))}
      </div>
      {/* Secondary content block */}
      <div className="bg-white rounded-xl border border-zinc-100 shadow-sm p-5">
        <Pulse className="h-4 w-32 mb-4" />
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {[1, 2].map((i) => (
            <div key={i} className="space-y-2">
              <Pulse className="h-3 w-24" />
              <Pulse className="h-8 w-full rounded-lg" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function FormSkeleton() {
  return (
    <div className="bg-white rounded-xl border border-zinc-100 shadow-sm p-6 space-y-6 max-w-2xl">
      {/* Section heading */}
      <div className="border-b border-zinc-100 pb-4">
        <Pulse className="h-4 w-32" />
      </div>
      {/* Fields */}
      {[1, 2, 3, 4].map((i) => (
        <div key={i} className="space-y-1.5">
          <Pulse className="h-3 w-20" />
          <Pulse className="h-9 w-full rounded-lg" />
        </div>
      ))}
      {/* Action buttons */}
      <div className="flex gap-2 pt-2">
        <Pulse className="h-9 w-24 rounded-lg" />
        <Pulse className="h-9 w-20 rounded-lg" />
      </div>
    </div>
  );
}

export function AdminPageSkeleton({ variant = "table", className }: AdminPageSkeletonProps) {
  return (
    <div className={cn("space-y-6", className)}>
      <PageHeaderSkeleton />
      {variant === "table" && <TableSkeleton />}
      {variant === "cards" && <CardsSkeleton />}
      {variant === "form" && <FormSkeleton />}
    </div>
  );
}
