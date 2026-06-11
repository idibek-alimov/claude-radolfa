import { Skeleton } from "@radolfa/shared/ui/skeleton";

export default function ProductCardSkeleton() {
  return (
    <div className="flex flex-col rounded-[14px] overflow-hidden border border-[rgba(14,17,22,0.07)] bg-card shadow-[0_1px_3px_rgba(14,17,22,0.06)]">
      <Skeleton className="w-full aspect-[3/4] rounded-none" />
      <div className="pt-[11px] px-[13px] pb-[10px] flex flex-col gap-2">
        <Skeleton className="h-5 w-20" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-3 w-2/3" />
      </div>
    </div>
  );
}
