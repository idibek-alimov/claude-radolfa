// Cross-app navigation between the storefront (dev :3000) and ops (dev :3001).
// Behind nginx (prod) both apps share one origin, so relative paths just work —
// these helpers are a no-op there. In dev they rewrite the origin to the target
// app's port. Convention is fixed in frontend/CLAUDE.md.
const DEV_PORTS = { storefront: "3000", ops: "3001" } as const;

function crossAppUrl(target: "storefront" | "ops", path: string): string {
  if (typeof window === "undefined") return path; // SSR-safe
  const { protocol, hostname, port } = window.location;
  const targetPort = DEV_PORTS[target];
  const onDevPort = port === DEV_PORTS.storefront || port === DEV_PORTS.ops;
  // Only rewrite when we're on a known dev port AND crossing to the other app.
  if (onDevPort && port !== targetPort) {
    return `${protocol}//${hostname}:${targetPort}${path}`;
  }
  return path; // same app in dev, or behind nginx in prod
}

export const storefrontUrl = (path: string) => crossAppUrl("storefront", path);
export const opsUrl = (path: string) => crossAppUrl("ops", path);

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
