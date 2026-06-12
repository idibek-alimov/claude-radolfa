import createNextIntlPlugin from "next-intl/plugin";
import { fileURLToPath } from "url";
import path from "path";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// next-intl request config lives in @radolfa/shared
const withNextIntl = createNextIntlPlugin(
  "../../packages/shared/src/i18n/request.ts"
);

/** @type {import('next').NextConfig} */
const nextConfig = {
  // Serve the ops portal under /ops — single-domain ingress via reverse proxy.
  // Reverse proxy: /ops → ops app, / → storefront.
  basePath: "/ops",

  // Enable standalone output for Docker deployment (smaller image size)
  output: "standalone",

  // Point file-tracing root at the workspace root so the standalone bundle
  // resolves hoisted node_modules from frontend/ (the npm workspace root).
  outputFileTracingRoot: path.join(__dirname, "../../"),

  // Let Next.js compile the local workspace package (not published to npm).
  transpilePackages: ["@radolfa/shared"],

  // Proxy API requests to the backend.
  // Local dev (next dev): BACKEND_INTERNAL_URL unset → falls back to localhost:8080
  // Docker: BACKEND_INTERNAL_URL=http://backend:8080 (set in docker-compose.yml)
  async rewrites() {
    const backendUrl =
      process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080";
    return {
      // basePath: false skips the automatic /ops prefix on this rewrite source,
      // so browser requests to /api/:path* are proxied correctly even though
      // the app's basePath is /ops.
      beforeFiles: [
        {
          source: "/api/:path*",
          destination: `${backendUrl}/api/:path*`,
          basePath: false,
        },
      ],
    };
  },

  images: {
    remotePatterns: [
      {
        protocol: "http",
        hostname: "localhost",
      },
      {
        protocol: "https",
        hostname: "images.unsplash.com",
      },
      {
        protocol: "https",
        hostname: "s3.twcstorage.ru",
      },
      {
        protocol: "https",
        hostname: "*.s3.amazonaws.com",
        pathname: "/products/**",
      },
    ],
    unoptimized: true,
  },
};

export default withNextIntl(nextConfig);
