# =============================================================================
# Radolfa Frontend - Multi-stage Dockerfile
# Stage 1: Dependencies
# Stage 2: Build
# Stage 3: Runtime (standalone output)
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Dependencies
# -----------------------------------------------------------------------------
FROM node:20-alpine AS deps

WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm ci

# -----------------------------------------------------------------------------
# Stage 2: Build
# -----------------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependencies from deps stage
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Set environment variables for build
ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_ENV=production

# Build the application (outputs to .next/standalone with output: 'standalone')
RUN npm run build

# -----------------------------------------------------------------------------
# Stage 3: Runtime (standalone)
# -----------------------------------------------------------------------------
FROM node:20-alpine AS runner

WORKDIR /app

# Create non-root user for security
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Set environment variables
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# Copy standalone output
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

# Copy public folder if it exists (optional assets)
# Using a workaround since COPY fails if source doesn't exist
COPY --from=builder --chown=nextjs:nodejs /app/package.json ./package.json.bak
RUN rm ./package.json.bak

# Switch to non-root user
USER nextjs

# Expose the application port
EXPOSE 3000

# Set the port
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:3000 || exit 1

# Start the standalone server
CMD ["node", "server.js"]
