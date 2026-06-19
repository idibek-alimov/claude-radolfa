import { NextResponse, type NextRequest } from "next/server";

// Single source of truth for the ops origin — mirrors appNav.ts default.
const OPS_ORIGIN =
  process.env.NEXT_PUBLIC_OPS_ORIGIN ?? "http://ops.localhost:3001";

// Dev-only host guard. The ops portal must be reached on the `ops.localhost` host so
// its auth cookie is scoped apart from the storefront on `localhost`. Opening it on
// plain `localhost:3001` shares the cookie with the storefront on `localhost:3000`
// (same host — port is ignored for host-only cookies) and silently defeats the origin
// split. Bounce those requests to the correct host. Inert in prod (manage.radolfa.site).
export function middleware(request: NextRequest) {
  const host = request.headers.get("host");
  if (host === "localhost:3001" || host === "127.0.0.1:3001") {
    const target = new URL(request.url); // keeps /ops/... path + query
    const ops = new URL(OPS_ORIGIN);
    target.protocol = ops.protocol;
    target.host = ops.host;
    return NextResponse.redirect(target, 307); // temporary — don't let the browser cache it
  }
  return NextResponse.next();
}

export const config = {
  // Exclude the /api proxy rewrite and Next internals; only guard page navigations.
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico).*)"],
};
