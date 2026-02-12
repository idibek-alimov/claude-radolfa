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
          {/* Dot indicators — small screens */}
          <div className="flex sm:hidden justify-center gap-2 mt-3">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="w-2.5 h-2.5 rounded-full" />
            ))}
          </div>
          {/* Thumbnails — sm+ screens */}
          <div className="hidden sm:flex gap-3 mt-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="w-20 h-20 rounded-lg" />
            ))}
          </div>
        </div>

        {/* Right — product info */}
        <div className="lg:col-span-2 space-y-6">
          <Skeleton className="h-10 w-3/4" />
          <Skeleton className="h-10 w-40" />
          <Skeleton className="h-6 w-28 rounded-full" />

          {/* Colour swatches */}
          <div className="pt-5 space-y-3">
            <Skeleton className="h-4 w-28" />
            <div className="flex gap-2">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="w-12 h-12 rounded-lg" />
              ))}
            </div>
          </div>

          {/* Size selector */}
          <div className="pt-5 space-y-3">
            <Skeleton className="h-4 w-24" />
            <div className="flex gap-2">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="w-16 h-10 rounded-full" />
              ))}
            </div>
          </div>

          {/* Description */}
          <div className="pt-5 space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-2/3" />
          </div>
        </div>
      </div>

      {/* Related products section */}
      <div className="mt-16 pt-16 border-t">
        <Skeleton className="h-7 w-48 mb-8" />
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6">
          {Array.from({ length: 4 }).map((_, i) => (
            <div
              key={i}
              className="rounded-xl border bg-card shadow overflow-hidden flex flex-col"
            >
              <Skeleton className="w-full h-48 rounded-none" />
              <div className="p-4 flex flex-col gap-3">
                <Skeleton className="h-5 w-3/4" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-2/3" />
                <div className="mt-auto flex items-center justify-between pt-2">
                  <Skeleton className="h-6 w-20" />
                  <Skeleton className="h-5 w-24 rounded-full" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
