# =============================================================================
# Radolfa Storefront - Multi-stage Dockerfile
# Build context: frontend/ (the npm workspace root)
#
# Stage 1: Dependencies  — install all workspace deps via npm ci
# Stage 2: Build         — build @radolfa/storefront
# Stage 3: Runtime       — run the standalone server
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Dependencies
# -----------------------------------------------------------------------------
FROM node:20-alpine AS deps

WORKDIR /app

# Copy workspace root manifests
COPY package.json package-lock.json ./

# Copy the app package manifest so npm ci resolves it as a workspace member
COPY apps/storefront/package.json ./apps/storefront/package.json

# Install all workspace dependencies (hoisted to /app/node_modules)
RUN npm ci

# -----------------------------------------------------------------------------
# Stage 2: Build
# -----------------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Copy hoisted node_modules from deps stage
COPY --from=deps /app/node_modules ./node_modules

# Copy the full workspace tree (source + config)
COPY . .

# Build-time environment
ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_ENV=production

# NEXT_PUBLIC_* vars are inlined at build time — pass via --build-arg in CI
ARG NEXT_PUBLIC_API_BASE_URL=https://api.radolfa.site
ARG NEXT_PUBLIC_API_URL=https://api.radolfa.site
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

# Cross-app navigation origins (appNav.ts) — pass via --build-arg in CI
ARG NEXT_PUBLIC_STOREFRONT_ORIGIN=https://radolfa.site
ARG NEXT_PUBLIC_OPS_ORIGIN=https://manage.radolfa.site
ENV NEXT_PUBLIC_STOREFRONT_ORIGIN=$NEXT_PUBLIC_STOREFRONT_ORIGIN
ENV NEXT_PUBLIC_OPS_ORIGIN=$NEXT_PUBLIC_OPS_ORIGIN

# Internal backend URL for Next.js server-side rewrites (not browser-visible)
ARG BACKEND_INTERNAL_URL=http://localhost:8080
ENV BACKEND_INTERNAL_URL=$BACKEND_INTERNAL_URL

# Build the storefront workspace app
RUN npm run build -w @radolfa/storefront

# -----------------------------------------------------------------------------
# Stage 3: Runtime (standalone)
# The standalone output mirrors the monorepo tree under .next/standalone,
# so the entry point is apps/storefront/server.js relative to the output root.
# -----------------------------------------------------------------------------
FROM node:20-alpine AS runner

WORKDIR /app

# Create non-root user for security
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Runtime environment
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# Copy the full standalone tree (includes all workspace dependencies)
COPY --from=builder --chown=nextjs:nodejs /app/apps/storefront/.next/standalone ./

# Copy the static assets next to where the standalone server expects them
COPY --from=builder --chown=nextjs:nodejs /app/apps/storefront/.next/static ./apps/storefront/.next/static

# Switch to non-root user
USER nextjs

# Expose the application port
EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:3000 || exit 1

# Start the standalone server
CMD ["node", "apps/storefront/server.js"]
