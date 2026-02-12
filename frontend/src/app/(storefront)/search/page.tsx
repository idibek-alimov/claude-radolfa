import { Search } from "lucide-react";

interface SearchPageProps {
  searchParams: { q?: string };
}

export default function SearchPage({ searchParams }: SearchPageProps) {
  const query = searchParams.q?.trim();

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {query ? (
        <>
          <h1 className="text-2xl sm:text-3xl font-semibold text-foreground tracking-tight">
            Results for{" "}
            <span className="text-primary">&ldquo;{query}&rdquo;</span>
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Searching across all products
          </p>

          {/* Placeholder for future product grid */}
          <div className="mt-16 flex flex-col items-center text-center">
            <div className="flex items-center justify-center h-16 w-16 rounded-2xl bg-muted/60 mb-6">
              <Search className="h-7 w-7 text-muted-foreground/60" />
            </div>
            <p className="text-lg font-medium text-foreground">
              Results coming soon
            </p>
            <p className="mt-1.5 text-sm text-muted-foreground max-w-sm">
              We&apos;re building the search experience. Product results for
              &ldquo;{query}&rdquo; will appear here.
            </p>
          </div>
        </>
      ) : (
        <div className="mt-24 flex flex-col items-center text-center">
          <div className="flex items-center justify-center h-20 w-20 rounded-2xl bg-muted/60 mb-6">
            <Search className="h-9 w-9 text-muted-foreground/50" />
          </div>
          <h1 className="text-2xl font-semibold text-foreground">
            Start your search
          </h1>
          <p className="mt-2 text-sm text-muted-foreground max-w-xs">
            Use the search bar above to find products across the Radolfa
            catalog.
          </p>
        </div>
      )}
    </div>
  );
}
