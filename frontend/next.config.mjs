/** @type {import('next').NextConfig} */
const nextConfig = {
  // Enable standalone output for Docker deployment (smaller image size)
  output: "standalone",
  images: {
    // S3 bucket domain — allow next/image to load product images from here.
    remotePatterns: [
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

export default nextConfig;
