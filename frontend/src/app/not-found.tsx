import Link from "next/link";
import { Home } from "lucide-react";

export default function NotFound() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-6 px-4 text-center">
      <p className="text-7xl font-extrabold text-primary/20 select-none">404</p>
      <div className="space-y-2">
        <h2 className="text-2xl font-semibold text-foreground">Page not found</h2>
        <p className="text-muted-foreground max-w-md">
          The page you are looking for does not exist or has been moved.
        </p>
      </div>
      <Link
        href="/"
        className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
      >
        <Home className="h-4 w-4" />
        Back to home
      </Link>
    </div>
  );
}
