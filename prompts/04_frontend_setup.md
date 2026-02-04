# PROMPT 4: FRONTEND SETUP (NEXT.JS 14)
# ============================================================
# GOAL: Initialize Frontend with Next.js 14 and TanStack Query.
#
# INSTRUCTIONS
# ------------------------------------------------------------
# 1. Initialize Next.js 14 App
#    - App Router (`/app` dir).
#    - TypeScript.
#    - Tailwind CSS.
#
# 2. Setup TanStack Query (React Query)
#    - Wrap root layout in `QueryClientProvider`.
#    - Create a standard fetcher utility (axios) with base URL from env.
#
# 3. Core Components
#    - `ProductCard`: Displays image, name, price.
#    - `ProductGrid`: Infinite scroll or paginated list.
#    - `Navbar`: Links to Home, Search, Login.
#
# 4. Pages
#    - `/`: Homepage (Top selling products).
#    - `/products`: Catalog with filters.
#    - `/products/[id]`: Detail page.
#
# 5. Image Handling
#    - Use `next/image`.
#    - Domain config: Allow AWS S3 bucket domain.
#    - `unoptimized` might be needed if not using a paid Vercel-like optimizer, OR use a custom loader if hosting on VPS to avoid Next.js server load.
#    - Recommendation: Set `unoptimized: true` for MVP to save CPU on VPS.
