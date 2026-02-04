# Radolfa Frontend — FSD Guardrails
- **Pattern:** Feature-Sliced Design (FSD).
- **Stack:** Next.js 14 (App Router), TanStack Query.
- **Authority:** Never allow editing of `price` or `name` in the UI.
- **Images:** Use `next/image` with `unoptimized: true` for S3 assets.
- **Rules:** - Cross-slice imports are forbidden.
  - Logic must live in `features/` or `entities/`, never in `pages/`.