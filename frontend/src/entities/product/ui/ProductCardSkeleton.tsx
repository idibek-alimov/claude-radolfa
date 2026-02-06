import { Skeleton } from "@/shared/ui/skeleton";

export default function ProductCardSkeleton() {
  return (
    <div className="rounded-xl border bg-card shadow overflow-hidden flex flex-col">
      <Skeleton className="w-full h-48 rounded-none" />
      <div className="p-4 flex flex-col flex-1 gap-3">
        <Skeleton className="h-5 w-3/4" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-2/3" />
        <div className="mt-auto flex items-center justify-between pt-2">
          <Skeleton className="h-6 w-20" />
          <Skeleton className="h-5 w-24 rounded-full" />
        </div>
      </div>
    </div>
  );
}
