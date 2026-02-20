"use client";

import { useEffect } from "react";
import { RefreshCcw } from "lucide-react";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("[ErrorBoundary]", error);
  }, [error]);

  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-6 px-4 text-center">
      <p className="text-7xl font-extrabold text-destructive/20 select-none">!</p>
      <div className="space-y-2">
        <h2 className="text-2xl font-semibold text-foreground">Something went wrong</h2>
        <p className="text-muted-foreground max-w-md">
          An unexpected error occurred. Please try again.
        </p>
      </div>
      <button
        onClick={reset}
        className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
      >
        <RefreshCcw className="h-4 w-4" />
        Try again
      </button>
    </div>
  );
}
