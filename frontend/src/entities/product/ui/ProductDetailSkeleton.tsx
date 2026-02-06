import { Skeleton } from "@/shared/ui/skeleton";

export default function ProductDetailSkeleton() {
  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Breadcrumb skeleton */}
      <div className="flex items-center gap-2 mb-8">
        <Skeleton className="h-4 w-12" />
        <Skeleton className="h-4 w-4" />
        <Skeleton className="h-4 w-16" />
        <Skeleton className="h-4 w-4" />
        <Skeleton className="h-4 w-32" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-10">
        {/* Left — image gallery */}
        <div className="lg:col-span-3">
          <Skeleton className="w-full aspect-square rounded-xl" />
          <div className="flex gap-3 mt-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="w-20 h-20 rounded-lg" />
            ))}
          </div>
        </div>

        {/* Right — product info */}
        <div className="lg:col-span-2 space-y-4">
          <Skeleton className="h-8 w-3/4" />
          <Skeleton className="h-8 w-32" />
          <Skeleton className="h-6 w-28 rounded-full" />
          <div className="pt-4 space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-2/3" />
          </div>
        </div>
      </div>
    </div>
  );
}
