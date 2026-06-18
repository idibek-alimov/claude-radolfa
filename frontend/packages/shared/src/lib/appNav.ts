// Cross-app navigation between the storefront and ops portal, which live on
// distinct origins in both dev (localhost:3000 / ops.localhost:3001) and prod
// (radolfa.site / manage.radolfa.site). Origins are build-time env vars so
// NEXT_PUBLIC_* inlining works in both Server and Client components.
const STOREFRONT_ORIGIN =
  process.env.NEXT_PUBLIC_STOREFRONT_ORIGIN ?? "http://localhost:3000";
const OPS_ORIGIN =
  process.env.NEXT_PUBLIC_OPS_ORIGIN ?? "http://ops.localhost:3001";

export const storefrontUrl = (path: string) => `${STOREFRONT_ORIGIN}${path}`;
export const opsUrl = (path: string) => `${OPS_ORIGIN}${path}`;

// Login page that belongs to the CURRENT app — keeps auth redirects same-app.
export function currentAppLoginPath(): string {
  return typeof window !== "undefined" &&
    window.location.pathname.startsWith("/ops")
    ? "/ops/login"
    : "/login";
}

export function currentAppHomePath(): string {
  return typeof window !== "undefined" &&
    window.location.pathname.startsWith("/ops")
    ? "/ops"
    : "/";
}
