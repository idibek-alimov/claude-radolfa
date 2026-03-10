import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin("./src/shared/i18n/request.ts");

/** @type {import('next').NextConfig} */
const nextConfig = {
  // Enable standalone output for Docker deployment (smaller image size)
  output: "standalone",

  // Proxy API requests to the backend.
  // Local dev (next dev): BACKEND_INTERNAL_URL unset → falls back to localhost:8080
  // Docker: BACKEND_INTERNAL_URL=http://backend:8080 (set in docker-compose.yml)
  async rewrites() {
    const backendUrl =
      process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080";
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
    ];
  },
  images: {
    // S3 bucket domain — allow next/image to load product images from here.
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
    // MVP on a single VPS: skip Next.js image optimization to save CPU.
    // Re-enable (remove this line) when a dedicated image-optimisation
    // layer (e.g. Cloudflare or imgproxy) is in front of the origin.
    unoptimized: true,
  },
};

export default withNextIntl(nextConfig);
